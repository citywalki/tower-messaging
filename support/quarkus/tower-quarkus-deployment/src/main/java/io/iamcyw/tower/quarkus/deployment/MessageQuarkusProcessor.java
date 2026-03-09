package io.iamcyw.tower.quarkus.deployment;

import io.iamcyw.tower.messaging.CommandHandler;
import io.iamcyw.tower.messaging.cdi.producer.MessageProducer;
import io.iamcyw.tower.messaging.spi.LookupService;
import io.iamcyw.tower.quarkus.runtime.MessageRecorder;
import io.iamcyw.tower.quarkus.runtime.QuarkusHandlerRegistry;
import io.iamcyw.tower.schema.Annotations;
import io.iamcyw.tower.schema.HandlerTypeResolver;
import io.iamcyw.tower.schema.ScanningContext;
import io.iamcyw.tower.schema.SchemaBuilder;
import io.iamcyw.tower.schema.creator.ArgumentCreator;
import io.iamcyw.tower.schema.creator.OperationCreator;
import io.iamcyw.tower.schema.creator.ReferenceCreator;
import io.iamcyw.tower.schema.model.Argument;
import io.iamcyw.tower.schema.model.HandlerTypeInfo;
import io.iamcyw.tower.schema.model.Operation;
import io.iamcyw.tower.schema.model.Reference;
import io.iamcyw.tower.schema.model.Schema;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.ExcludedTypeBuildItem;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.annotations.*;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.TransformedClassesBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.RuntimeValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.jboss.logging.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * Quarkus deployment processor for Tower Messaging framework.
 *
 * <p>This processor handles build-time discovery and registration of command handlers
 * for the interface-based CommandHandler pattern. It performs the following:</p>
 *
 * <ul>
 *   <li>Scans for {@link CommandHandler} implementations using Jandex</li>
 *   <li>Extracts generic type parameters using {@link HandlerTypeResolver}</li>
 *   <li>Generates {@code HandlerMetadata} classes using bytecode generation</li>
 *   <li>Registers handlers and metadata as CDI beans</li>
 *   <li>Configures the {@link io.iamcyw.tower.messaging.HandlerRegistry}</li>
 * </ul>
 *
 * @see CommandHandler
 * @see HandlerTypeResolver
 * @see HandlerMetadataGenerator
 * @since 2.0
 */
public class MessageQuarkusProcessor {

    private static final Logger LOG = Logger.getLogger(MessageQuarkusProcessor.class);

    private static final String FEATURE = "tower";

    private static final DotName COMMAND_HANDLER_NAME = DotName.createSimple(CommandHandler.class.getName());

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    /**
     * Excludes the HandlerRegistryProducer from CDI discovery.
     *
     * <p>This build step excludes the {@code HandlerRegistryProducer} from the messaging-cdi
     * module because the Quarkus extension provides its own {@link QuarkusHandlerRegistry}
     * implementation. This prevents ambiguous dependency errors when both the producer
     * and the Quarkus-specific implementation are present.</p>
     *
     * <p>The {@link ExcludedTypeBuildItem} ensures that the specified class is not
     * considered a bean during CDI discovery, even if it has bean-defining annotations.</p>
     *
     * @param excludedTypes producer for excluded type build items
     */
    @BuildStep
    void excludeHandlerRegistryProducer(BuildProducer<ExcludedTypeBuildItem> excludedTypes) {
        excludedTypes.produce(new ExcludedTypeBuildItem(
                "io.iamcyw.tower.messaging.cdi.producer.HandlerRegistryProducer"));
    }

    @BuildStep
    void additionalBeanDefiningAnnotation(
            BuildProducer<BeanDefiningAnnotationBuildItem> beanDefiningAnnotationProducer) {
        // Make ArC discover the beans marked with the @GraphQlApi qualifier
        beanDefiningAnnotationProducer.produce(new BeanDefiningAnnotationBuildItem(Annotations.USECASE));
    }

    @BuildStep
    void additionalBean(BuildProducer<AdditionalBeanBuildItem> additionalBeanProducer) {
        additionalBeanProducer.produce(
                AdditionalBeanBuildItem.builder().addBeanClass(MessageProducer.class).setUnremovable().build());
    }

    @BuildStep
    void registerNativeImageResources(BuildProducer<ServiceProviderBuildItem> serviceProvider) {
        // Lookup Service (We use the one from the CDI Module)
        serviceProvider.produce(ServiceProviderBuildItem.allProvidersFromClassPath(LookupService.class.getName()));
    }

    @BuildStep
    TowerMessageIndexBuildItem createIndex(TransformedClassesBuildItem transformedClassesBuildItem) {
        Map<String, byte[]> modifiedClasses = new HashMap<>();
        Map<Path, Set<TransformedClassesBuildItem.TransformedClass>> transformedClassesByJar =
                transformedClassesBuildItem.getTransformedClassesByJar();
        for (Map.Entry<Path, Set<TransformedClassesBuildItem.TransformedClass>> transformedClassesByJarEntrySet :
                transformedClassesByJar.entrySet()) {

            Set<TransformedClassesBuildItem.TransformedClass> transformedClasses =
                    transformedClassesByJarEntrySet.getValue();
            for (TransformedClassesBuildItem.TransformedClass transformedClass : transformedClasses) {
                modifiedClasses.put(transformedClass.getClassName(), transformedClass.getData());
            }
        }
        return new TowerMessageIndexBuildItem(modifiedClasses);
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    void buildExecutionService(BuildProducer<ReflectiveClassBuildItem> reflectiveClassProducer,
                               BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer,
                               BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchyProducer,
                               BuildProducer<TowerMessageSchemaBuildItem> towerMessageSchemaBuildItemBuildProducer,
                               MessageRecorder recorder, TowerMessageIndexBuildItem towerMessageIndexBuildItem,
                               BeanContainerBuildItem beanContainer, CombinedIndexBuildItem combinedIndex) {

        Indexer indexer = new Indexer();
        Map<String, byte[]> modifiedClasses = towerMessageIndexBuildItem.getModifiedClases();

        for (Map.Entry<String, byte[]> kv : modifiedClasses.entrySet()) {
            if (kv.getKey() != null && kv.getValue() != null) {
                try (ByteArrayInputStream bais = new ByteArrayInputStream(kv.getValue())) {
                    indexer.index(bais);
                } catch (IOException ex) {
                    LOG.warn("Could not index [" + kv.getKey() + "] - " + ex.getMessage());
                }
            }
        }

        OverridableIndex overridableIndex = OverridableIndex.create(combinedIndex.getIndex(), indexer.complete());

        ReferenceCreator referenceCreator = new ReferenceCreator();
        ArgumentCreator argumentCreator = new ArgumentCreator(referenceCreator);
        MethodInvokerFactory methodInvokerFactory = new MethodInvokerFactory(generatedClassBuildItemBuildProducer);
        OperationCreator operationCreator = new OperationCreator(referenceCreator, argumentCreator,
                                                                 methodInvokerFactory::create);

        SchemaBuilder schemaBuilder = new SchemaBuilder(referenceCreator,operationCreator);
        ScanningContext.register(overridableIndex);
        Schema schema = schemaBuilder.generateSchema();

        RuntimeValue<Boolean> initialized = recorder.createMessageService(beanContainer.getValue(), schema);

        towerMessageSchemaBuildItemBuildProducer.produce(new TowerMessageSchemaBuildItem(schema));

        // Make sure the complex object from the application can work in native mode
        reflectiveClassProducer.produce(
                ReflectiveClassBuildItem.builder(getSchemaJavaClasses(schema)).methods(true).fields(true).build());
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    @Consume(BeanContainerBuildItem.class)
    void buildExecutionEndpoint(MessageRecorder recorder, ShutdownContextBuildItem shutdownContext,
                                LaunchModeBuildItem launchMode) {

        /*
         * <em>Ugly Hack</em>
         * In dev mode, we pass a classloader to use in the CDI Loader.
         * This hack is required because using the TCCL would get an outdated version - the initial one.
         * This is because the worker thread on which the handler is called captures the TCCL at creation time
         * and does not allow updating it.
         *
         * In non dev mode, the TCCL is used.
         */
        if (launchMode.getLaunchMode() == LaunchMode.DEVELOPMENT) {
            recorder.setupClDevMode(shutdownContext);
        }
    }

    private String[] getSchemaJavaClasses(Schema schema) {
        // Unique list of classes we need to do reflection on
        Set<String> classes = new HashSet<>();

        classes.addAll(getOperationClassNames(schema.getCommands()));

        return classes.toArray(new String[]{});
    }

    private Set<String> getOperationClassNames(Set<Operation> operations) {
        Set<String> classes = new HashSet<>();
        for (Operation operation : operations) {
            classes.add(operation.getClassName());
            for (Argument argument : operation.getArguments()) {
                classes.addAll(getAllReferenceClasses(argument.getReference()));
            }
            classes.addAll(getAllReferenceClasses(operation.getReference()));
        }
        return classes;
    }

    private Set<String> getAllReferenceClasses(Reference reference) {
        Set<String> classes = new HashSet<>();
        if (reference.getClassName().equals("void")) {
            return classes;
        }
        classes.add(reference.getClassName());
        if (reference.getParametrizedTypeArguments() != null && !reference.getParametrizedTypeArguments().isEmpty()) {

            Collection<Reference> parametrized = reference.getParametrizedTypeArguments().values();
            for (Reference r : parametrized) {
                classes.addAll(getAllReferenceClasses(r));
            }
        }
        return classes;
    }

    /**
     * Scans for CommandHandler implementations and generates metadata classes.
     *
     * <p>This build step runs during STATIC_INIT and performs the following:
     * <ol>
     *   <li>Scans the Jandex index for all implementations of {@link CommandHandler}</li>
     *   <li>Extracts generic type parameters (C and R) using {@link HandlerTypeResolver}</li>
     *   <li>Generates {@code {HandlerClass}$Metadata} classes using bytecode generation via
     *       {@link GeneratedBeanBuildItem} for proper CDI integration</li>
     *   <li>Produces {@link CommandHandlerBuildItem} for each discovered handler</li>
     *   <li>Registers handlers as CDI beans via {@link AdditionalBeanBuildItem}</li>
     * </ol>
     *
     * <p><strong>Note:</strong> We use {@link GeneratedBeanBuildItem} for metadata classes
     * because they are generated bytecode that needs to be proper CDI beans. This ensures
     * Quarkus's CDI container can properly index and manage them.
     *
     * @param combinedIndex      the combined Jandex index containing all application classes
     * @param handlerProducer    producer for CommandHandlerBuildItem
     * @param generatedBeans     producer for generated CDI bean build items (metadata classes)
     * @param additionalBeans    producer for additional CDI bean registrations (handler classes)
     */
    @BuildStep
    void scanCommandHandlers(
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<CommandHandlerBuildItem> handlerProducer,
            BuildProducer<GeneratedBeanBuildItem> generatedBeans,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {

        IndexView index = combinedIndex.getIndex();
        Collection<ClassInfo> handlers = index.getAllKnownImplementors(COMMAND_HANDLER_NAME);

        if (handlers.isEmpty()) {
            LOG.debug("No CommandHandler implementations found");
            return;
        }

        LOG.infof("Found %d CommandHandler implementations", handlers.size());

        HandlerTypeResolver resolver = new HandlerTypeResolver();
        HandlerMetadataGenerator metadataGenerator = new HandlerMetadataGenerator();

        for (ClassInfo handlerClass : handlers) {
            // Skip abstract classes - only concrete implementations
            if (isAbstract(handlerClass)) {
                LOG.debugf("Skipping abstract class: %s", handlerClass.name());
                continue;
            }

            // Skip interfaces - only class implementations
            if (isInterface(handlerClass)) {
                LOG.debugf("Skipping interface: %s", handlerClass.name());
                continue;
            }

            try {
                // Extract types using HandlerTypeResolver
                HandlerTypeInfo typeInfo = resolver.resolve(handlerClass, index);

                String handlerClassName = typeInfo.getHandlerClassName();
                String commandTypeName = typeInfo.getCommandType().name().toString();
                String resultTypeName = typeInfo.hasResultType()
                        ? typeInfo.getResultType().name().toString()
                        : null;
                String metadataClassName = handlerClassName + HandlerMetadataGenerator.METADATA_SUFFIX;

                LOG.debugf("Processing handler: %s -> command=%s, result=%s",
                           handlerClassName, commandTypeName, resultTypeName);

                // Generate metadata class as a GeneratedBeanBuildItem
                // This ensures proper CDI bean registration for generated classes
                metadataGenerator.generate(typeInfo, generatedBeans);

                // Register the handler class as a CDI bean
                // Note: Even if the class has @Singleton, we need to explicitly register it
                // to ensure it's included in the CDI bean archive
                additionalBeans.produce(AdditionalBeanBuildItem.builder()
                                                .addBeanClass(handlerClassName)
                                                .setUnremovable()
                                                .build());
                LOG.debugf("Registered handler as CDI bean: %s", handlerClassName);

                // Produce build item
                handlerProducer.produce(new CommandHandlerBuildItem(
                        handlerClassName,
                        commandTypeName,
                        resultTypeName,
                        metadataClassName
                ));

                LOG.debugf("Generated metadata class: %s", metadataClassName);

            } catch (Exception e) {
                LOG.warnf("Failed to process handler %s: %s", handlerClass.name(), e.getMessage());
                // Continue processing other handlers
            }
        }

        LOG.infof("Successfully processed %d CommandHandler implementations", handlers.size());
    }

    /**
     * Checks if a class is abstract.
     *
     * @param classInfo the class to check
     * @return true if the class is abstract
     */
    private boolean isAbstract(ClassInfo classInfo) {
        return (classInfo.flags() & java.lang.reflect.Modifier.ABSTRACT) != 0;
    }

    /**
     * Checks if a class is an interface.
     *
     * @param classInfo the class to check
     * @return true if the class is an interface
     */
    private boolean isInterface(ClassInfo classInfo) {
        return (classInfo.flags() & java.lang.reflect.Modifier.INTERFACE) != 0;
    }

    /**
     * Configures runtime components using discovered command handlers.
     *
     * <p>This build step consumes {@link CommandHandlerBuildItem}s and
     * {@link GeneratedBeanBuildItem}s produced by
     * {@link #scanCommandHandlers(CombinedIndexBuildItem, BuildProducer, BuildProducer, BuildProducer)}
     * and registers the {@link QuarkusHandlerRegistry} as a CDI bean. This ensures that
     * build-time discovery feeds into runtime registration, satisfying the Quarkus build
     * item pattern requirement.</p>
     *
     * <p>The method performs the following:</p>
     * <ol>
     *   <li>Consumes all CommandHandlerBuildItems produced during scanning</li>
     *   <li>Consumes all GeneratedBeanBuildItems (metadata classes) produced during scanning</li>
     *   <li>Registers QuarkusHandlerRegistry as an additional CDI bean</li>
     *   <li>Logs discovered handlers for debugging purposes</li>
     * </ol>
     *
     * <p><strong>Build Item Pattern:</strong> This method consumes both
     * {@link CommandHandlerBuildItem} and {@link GeneratedBeanBuildItem} to ensure
     * proper ordering - the metadata classes must be generated before the registry
     * is configured. This fixes the orphaned build item issue where
     * CommandHandlerBuildItem was produced but never consumed.</p>
     *
     * @param handlers         the list of discovered command handler build items
     * @param generatedBeans   the list of generated bean build items (metadata classes)
     * @param additionalBeans  producer for registering additional CDI beans
     */
    @BuildStep
    @Consume(CommandHandlerBuildItem.class)
    @Consume(GeneratedBeanBuildItem.class)
    void configureRuntime(
            List<CommandHandlerBuildItem> handlers,
            List<GeneratedBeanBuildItem> generatedBeans,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {

        LOG.infof("Configuring runtime with %d command handlers and %d generated metadata classes",
                  handlers.size(), generatedBeans.size());

        // Register QuarkusHandlerRegistry as a CDI bean
        additionalBeans.produce(AdditionalBeanBuildItem.builder()
                                        .addBeanClass(QuarkusHandlerRegistry.class)
                                        .setUnremovable()
                                        .build());

        // Log discovered handlers for debugging
        if (LOG.isDebugEnabled()) {
            for (CommandHandlerBuildItem handler : handlers) {
                LOG.debugf("Registered handler: %s -> command=%s, result=%s",
                           handler.getHandlerClass(),
                           handler.getCommandType(),
                           handler.getResultType());
            }
        }
    }

}
