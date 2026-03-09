package io.iamcyw.tower.messaging.cdi.producer;

import io.iamcyw.tower.messaging.HandlerRegistry;
import io.iamcyw.tower.messaging.gateway.DefaultMessageGateway;
import io.iamcyw.tower.messaging.gateway.MessageGateway;
import io.iamcyw.tower.messaging.interceptor.CommandInterceptor;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * CDI producer for {@link MessageGateway}.
 *
 * <p>This producer creates a {@link DefaultMessageGateway} using the
 * {@link HandlerRegistry} and all available {@link CommandInterceptor} instances
 * provided by CDI. The gateway is created as an application-scoped bean.</p>
 *
 * <p>Interceptors are automatically discovered from the CDI container and sorted
 * by their {@link Priority} annotation value (or {@link CommandInterceptor#order()}
 * if no annotation is present).</p>
 *
 * @since 2.0
 */
@Singleton
public class MessageGatewayProducer {

    private final HandlerRegistry handlerRegistry;
    private final Instance<CommandInterceptor> interceptorInstances;

    @Inject
    public MessageGatewayProducer(HandlerRegistry handlerRegistry,
                                  Instance<CommandInterceptor> interceptorInstances) {
        this.handlerRegistry = handlerRegistry;
        this.interceptorInstances = interceptorInstances;
    }

    @Produces
    @ApplicationScoped
    public MessageGateway produceMessageGateway() {
        List<CommandInterceptor> interceptors = collectInterceptors();
        return new DefaultMessageGateway(handlerRegistry, interceptors);
    }

    /**
     * Collects all command interceptors from the CDI container.
     *
     * <p>Interceptors are sorted by:
     * <ol>
     *   <li>{@link Priority} annotation value (lower values first)</li>
     *   <li>{@link CommandInterceptor#order()} method as fallback</li>
     * </ol>
     *
     * @return a sorted list of all CDI-managed interceptors
     */
    private List<CommandInterceptor> collectInterceptors() {
        List<CommandInterceptor> interceptors = new ArrayList<>();
        for (CommandInterceptor interceptor : interceptorInstances) {
            interceptors.add(interceptor);
        }

        // Sort by Priority annotation or order() method
        interceptors.sort(Comparator.comparingInt(this::getOrder));

        return interceptors;
    }

    /**
     * Gets the order value for an interceptor.
     *
     * <p>First checks for {@link Priority} annotation, then falls back to
     * {@link CommandInterceptor#order()} method.</p>
     *
     * @param interceptor the interceptor to get order for
     * @return the order value (lower values execute first)
     */
    private int getOrder(CommandInterceptor interceptor) {
        Priority priority = interceptor.getClass().getAnnotation(Priority.class);
        if (priority != null) {
            return priority.value();
        }
        return interceptor.order();
    }

}
