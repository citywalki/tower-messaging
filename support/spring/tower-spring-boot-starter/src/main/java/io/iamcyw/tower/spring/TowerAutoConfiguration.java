package io.iamcyw.tower.spring;

import io.iamcyw.tower.messaging.CommandHandler;
import io.iamcyw.tower.messaging.DefaultHandlerRegistry;
import io.iamcyw.tower.messaging.HandlerMetadata;
import io.iamcyw.tower.messaging.HandlerRegistry;
import io.iamcyw.tower.messaging.gateway.DefaultMessageGateway;
import io.iamcyw.tower.messaging.gateway.MessageGateway;
import io.iamcyw.tower.messaging.interceptor.CommandInterceptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.List;

/**
 * Spring Boot auto-configuration for Tower Messaging.
 *
 * <p>This configuration automatically discovers all {@link CommandHandler} beans
 * in the Spring application context and wires up the messaging infrastructure:</p>
 * <ul>
 *   <li>{@link HandlerRegistry} — built from discovered handlers and reflective metadata</li>
 *   <li>{@link MessageGateway} — configured with the registry and any {@link CommandInterceptor} beans</li>
 * </ul>
 *
 * <p>All beans use {@code @ConditionalOnMissingBean}, so users can override any
 * component by defining their own bean of the same type.</p>
 *
 * <p>{@link CommandHandlerScanRegistrar} automatically scans the application's base
 * packages for {@link CommandHandler} implementations, so handler classes do not
 * need any Spring annotations — consistent with Clean Architecture principles.</p>
 *
 * @since 2.0
 */
@AutoConfiguration
@Import(CommandHandlerScanRegistrar.class)
public class TowerAutoConfiguration {

    /**
     * Creates a {@link HandlerRegistry} from all discovered {@link CommandHandler} beans.
     *
     * <p>For each handler bean, a {@link ReflectiveHandlerMetadata} is created to
     * extract the generic type parameters ({@code C} and {@code R}) at runtime.
     * If explicit {@link HandlerMetadata} beans are registered, they take precedence.</p>
     *
     * @param handlerProvider provides all CommandHandler beans from the application context
     * @param metadataProvider provides any explicitly registered HandlerMetadata beans
     * @return a handler registry containing all discovered handlers
     */
    @Bean
    @ConditionalOnMissingBean
    public HandlerRegistry handlerRegistry(
            ObjectProvider<CommandHandler<?, ?>> handlerProvider,
            ObjectProvider<HandlerMetadata<?, ?>> metadataProvider) {

        List<CommandHandler<?, ?>> handlers = new ArrayList<>();
        handlerProvider.orderedStream().forEach(handlers::add);

        // Collect explicit metadata beans
        List<HandlerMetadata<?, ?>> metadata = new ArrayList<>();
        metadataProvider.orderedStream().forEach(metadata::add);

        // For handlers without explicit metadata, generate reflective metadata
        var handlersWithMetadata = metadata.stream()
                .map(HandlerMetadata::handlerClass)
                .collect(java.util.stream.Collectors.toSet());

        for (CommandHandler<?, ?> handler : handlers) {
            if (!handlersWithMetadata.contains(handler.getClass())) {
                metadata.add(new ReflectiveHandlerMetadata<>(handler));
            }
        }

        return new DefaultHandlerRegistry(handlers, metadata);
    }

    /**
     * Creates a {@link MessageGateway} with the handler registry and interceptors.
     *
     * <p>Interceptors are sorted by {@link CommandInterceptor#order()} (lower values
     * execute first). The sorting is handled by {@link DefaultMessageGateway}.</p>
     *
     * @param handlerRegistry the handler registry
     * @param interceptorProvider provides all CommandInterceptor beans
     * @return a configured message gateway
     */
    @Bean
    @ConditionalOnMissingBean
    public MessageGateway messageGateway(
            HandlerRegistry handlerRegistry,
            ObjectProvider<CommandInterceptor> interceptorProvider) {

        List<CommandInterceptor> interceptors = new ArrayList<>();
        interceptorProvider.forEach(interceptors::add);

        return new DefaultMessageGateway(handlerRegistry, interceptors);
    }

}
