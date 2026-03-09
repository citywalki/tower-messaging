package io.iamcyw.tower.quarkus.deployment;

import io.iamcyw.tower.messaging.Command;
import io.iamcyw.tower.messaging.CommandHandler;
import io.iamcyw.tower.messaging.HandlerMetadata;
import io.iamcyw.tower.quarkus.deployment.resolver.HandlerTypeInfo;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.ResultHandle;
import jakarta.inject.Singleton;
import org.jboss.jandex.ClassInfo;
import org.jboss.logging.Logger;

/**
 * Generates {@code HandlerMetadata} implementation classes using Gizmo bytecode generation.
 *
 * <p>This generator creates {@code {HandlerClass}$Metadata} classes at build time for each
 * discovered {@code CommandHandler} implementation. The generated classes implement
 * {@link HandlerMetadata} and provide runtime access to the generic type parameters
 * (command type C and result type R) without reflection.</p>
 *
 * <p>Generated class structure:
 * <pre>
 * {@code @Singleton}
 * public class CreateOrderHandler$Metadata implements HandlerMetadata&lt;CreateOrderCommand, OrderResult&gt; {
 *
 *     {@code @Override}
 *     public Class&lt;CreateOrderCommand&gt; commandType() {
 *         return CreateOrderCommand.class;
 *     }
 *
 *     {@code @Override}
 *     public Class&lt;OrderResult&gt; resultType() {
 *         return OrderResult.class;
 *     }
 *
 *     {@code @Override}
 *     public String handlerName() {
 *         return "com.example.CreateOrderHandler";
 *     }
 *
 *     {@code @Override}
 *     public Class&lt;CreateOrderHandler&gt; handlerClass() {
 *         return CreateOrderHandler.class;
 *     }
 * }
 * </pre></p>
 *
 * <p>The generated class is registered as a CDI bean via {@link GeneratedBeanBuildItem},
 * allowing it to be injected at runtime for handler discovery and routing. Using
 * {@code GeneratedBeanBuildItem} ensures the generated class is properly indexed by
 * Quarkus's CDI container.</p>
 *
 * @see HandlerMetadata
 * @see CommandHandler
 * @see HandlerTypeInfo
 * @see GeneratedBeanBuildItem
 * @since 2.0
 */
public class HandlerMetadataGenerator {

    private static final Logger LOG = Logger.getLogger(HandlerMetadataGenerator.class);

    /**
     * Suffix used for generated metadata class names.
     */
    public static final String METADATA_SUFFIX = "$Metadata";

    /**
     * Generates a {@code HandlerMetadata} implementation class for the given handler type info.
     *
     * <p>This method uses Gizmo to generate bytecode for a class that implements
     * {@code HandlerMetadata<C, R>} where C is the command type and R is the result type
     * extracted from the handler's implementation of {@code CommandHandler<C, R>}.</p>
     *
     * <p>The generated class is named {@code {HandlerClassName}$Metadata} and is annotated
     * with {@code @Singleton} for CDI integration. It is registered as a generated bean
     * via the {@code generatedBeans} producer using {@link GeneratedBeanBuildItem}.</p>
     *
     * <p><strong>Note:</strong> We use {@link GeneratedBeanBuildItem} instead of
     * {@code GeneratedClassBuildItem} + {@code AdditionalBeanBuildItem} because
     * {@code GeneratedBeanBuildItem} properly handles both class generation and CDI
     * bean registration in a single build item, ensuring the generated class is
     * correctly indexed by Quarkus's CDI container.</p>
     *
     * @param handlerInfo       the handler type information containing command and result types, must not be null
     * @param generatedBeans    the build producer for generated CDI beans, must not be null
     * @throws NullPointerException if any parameter is null
     */
    public void generate(HandlerTypeInfo handlerInfo,
                         BuildProducer<GeneratedBeanBuildItem> generatedBeans) {

        if (handlerInfo == null) {
            throw new NullPointerException("handlerInfo must not be null");
        }
        if (generatedBeans == null) {
            throw new NullPointerException("generatedBeans must not be null");
        }

        String handlerClassName = handlerInfo.getHandlerClassName();
        String metadataClassName = handlerClassName + METADATA_SUFFIX;

        ClassInfo commandTypeInfo = handlerInfo.getCommandType();
        ClassInfo resultTypeInfo = handlerInfo.getResultType();

        String commandTypeName = commandTypeInfo != null ? commandTypeInfo.name().toString() : null;
        String resultTypeName = resultTypeInfo != null ? resultTypeInfo.name().toString() : "void";

        // Note: This method generates metadata classes, but they may not be properly
        // registered with the CDI container due to limitations with GeneratedBeanBuildItem.
        // As a fallback, QuarkusHandlerRegistry uses ReflectiveHandlerMetadata to extract
        // type information from handlers at runtime.
        LOG.debugf("Generating metadata class: %s for handler: %s", metadataClassName, handlerClassName);

        // Use GeneratedBeanGizmoAdaptor to produce GeneratedBeanBuildItem
        ClassOutput classOutput = new GeneratedBeanGizmoAdaptor(generatedBeans);

        try (ClassCreator cc = ClassCreator.builder()
                .classOutput(classOutput)
                .className(metadataClassName)
                .interfaces(HandlerMetadata.class)
                .build()) {

            // Add @Singleton annotation for CDI
            cc.addAnnotation(Singleton.class);

            // Generate commandType() method
            MethodCreator commandTypeMethod = cc.getMethodCreator("commandType", Class.class);
            commandTypeMethod.setModifiers(java.lang.reflect.Modifier.PUBLIC);
            if (commandTypeName != null) {
                ResultHandle commandClassHandle = commandTypeMethod.loadClass(commandTypeName);
                commandTypeMethod.returnValue(commandClassHandle);
            } else {
                commandTypeMethod.returnValue(commandTypeMethod.loadNull());
            }

            // Generate resultType() method
            MethodCreator resultTypeMethod = cc.getMethodCreator("resultType", Class.class);
            resultTypeMethod.setModifiers(java.lang.reflect.Modifier.PUBLIC);
            if (resultTypeName != null && !"void".equals(resultTypeName)) {
                ResultHandle resultClassHandle = resultTypeMethod.loadClass(resultTypeName);
                resultTypeMethod.returnValue(resultClassHandle);
            } else {
                resultTypeMethod.returnValue(resultTypeMethod.loadNull());
            }

            // Generate handlerName() method
            MethodCreator handlerNameMethod = cc.getMethodCreator("handlerName", String.class);
            handlerNameMethod.setModifiers(java.lang.reflect.Modifier.PUBLIC);
            ResultHandle nameHandle = handlerNameMethod.load(handlerClassName);
            handlerNameMethod.returnValue(nameHandle);

            // Generate handlerClass() method
            MethodCreator handlerClassMethod = cc.getMethodCreator("handlerClass", Class.class);
            handlerClassMethod.setModifiers(java.lang.reflect.Modifier.PUBLIC);
            ResultHandle handlerClassHandle = handlerClassMethod.loadClass(handlerClassName);
            handlerClassMethod.returnValue(handlerClassHandle);

        }
    }

}
