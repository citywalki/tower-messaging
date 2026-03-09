package io.iamcyw.tower.quarkus.deployment;

import io.iamcyw.tower.messaging.CommandHandler;
import io.iamcyw.tower.messaging.interceptor.CommandInterceptor;
import io.iamcyw.tower.quarkus.runtime.QuarkusHandlerRegistry;
import io.iamcyw.tower.schema.HandlerTypeResolver;
import io.iamcyw.tower.schema.model.HandlerTypeInfo;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.arc.deployment.ExcludedTypeBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.Collection;
import java.util.List;

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
    private static final DotName COMMAND_INTERCEPTOR_NAME = DotName.createSimple(CommandInterceptor.class.getName());

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
     * @param excludedTypes producer for excluded type build items
     */
    @BuildStep
    void excludeHandlerRegistryProducer(io.quarkus.deployment.annotations.BuildProducer<ExcludedTypeBuildItem> excludedTypes) {
        excludedTypes.produce(new ExcludedTypeBuildItem(
                "io.iamcyw.tower.messaging.cdi.producer.HandlerRegistryProducer"));
    }

    /**
     * Registers the {@link CommandHandler} and {@link CommandInterceptor} interfaces
     * as bean defining annotations.
     *
     * <p>This allows implementing classes to be discovered as CDI beans
     * even without explicit bean defining annotations.</p>
     *
     * @param beanDefiningAnnotationProducer producer for bean defining annotations
     */
    @BuildStep
    void additionalBeanDefiningAnnotation(
            io.quarkus.deployment.annotations.BuildProducer<BeanDefiningAnnotationBuildItem> beanDefiningAnnotationProducer) {
        beanDefiningAnnotationProducer.produce(
                new BeanDefiningAnnotationBuildItem(COMMAND_HANDLER_NAME, DotName.createSimple(ApplicationScoped.class.getName())));
        beanDefiningAnnotationProducer.produce(
                new BeanDefiningAnnotationBuildItem(COMMAND_INTERCEPTOR_NAME, DotName.createSimple(ApplicationScoped.class.getName())));
    }

    /**
     * Scans for CommandHandler implementations and generates metadata classes.
     *
     * <p>This build step performs the following:
     * <ol>
     *   <li>Scans the Jandex index for all implementations of {@link CommandHandler}</li>
     *   <li>Extracts generic type parameters (C and R) using {@link HandlerTypeResolver}</li>
     *   <li>Generates {@code {HandlerClass}$Metadata} classes using bytecode generation</li>
     *   <li>Produces {@link CommandHandlerBuildItem} for each discovered handler</li>
     *   <li>Registers handlers as CDI beans via {@link AdditionalBeanBuildItem}</li>
     * </ol>
     *
     * @param combinedIndex      the combined Jandex index containing all application classes
     * @param handlerProducer    producer for CommandHandlerBuildItem
     * @param generatedBeans     producer for generated CDI bean build items (metadata classes)
     * @param additionalBeans    producer for additional CDI bean registrations
     */
    @BuildStep
    void scanCommandHandlers(
            CombinedIndexBuildItem combinedIndex,
            io.quarkus.deployment.annotations.BuildProducer<CommandHandlerBuildItem> handlerProducer,
            io.quarkus.deployment.annotations.BuildProducer<GeneratedBeanBuildItem> generatedBeans,
            io.quarkus.deployment.annotations.BuildProducer<AdditionalBeanBuildItem> additionalBeans) {

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
                metadataGenerator.generate(typeInfo, generatedBeans);

                // Register the handler class as a CDI bean
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
     * Configures runtime components using discovered command handlers.
     *
     * <p>This build step consumes {@link CommandHandlerBuildItem}s and
     * {@link GeneratedBeanBuildItem}s produced by scanCommandHandlers
     * and registers the {@link QuarkusHandlerRegistry} as a CDI bean.</p>
     *
     * @param handlers         the list of discovered command handler build items
     * @param generatedBeans   the list of generated bean build items
     * @param additionalBeans  producer for registering additional CDI beans
     */
    @BuildStep
    @Consume(CommandHandlerBuildItem.class)
    @Consume(GeneratedBeanBuildItem.class)
    void configureRuntime(
            List<CommandHandlerBuildItem> handlers,
            List<GeneratedBeanBuildItem> generatedBeans,
            io.quarkus.deployment.annotations.BuildProducer<AdditionalBeanBuildItem> additionalBeans) {

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

}
