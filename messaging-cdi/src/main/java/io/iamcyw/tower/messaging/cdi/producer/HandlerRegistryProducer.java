package io.iamcyw.tower.messaging.cdi.producer;

import io.iamcyw.tower.messaging.CommandHandler;
import io.iamcyw.tower.messaging.DefaultHandlerRegistry;
import io.iamcyw.tower.messaging.HandlerMetadata;
import io.iamcyw.tower.messaging.HandlerRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.List;

/**
 * CDI producer for {@link HandlerRegistry} that collects all command handlers and their metadata.
 *
 * <p>This producer automatically discovers all CDI-managed {@link CommandHandler} implementations
 * and their associated {@link HandlerMetadata} to create a {@link DefaultHandlerRegistry}.
 * The registry is created as an application-scoped bean, ensuring a single instance is shared
 * across the application.</p>
 *
 * <p>Usage example:</p>
 * <pre>
 * // In a CDI bean, inject the registry
 * &#64;Inject
 * HandlerRegistry handlerRegistry;
 *
 * // Use the registry to find handlers for a command
 * List&lt;CommandHandler&lt;MyCommand, MyResult&gt;&gt; handlers =
 *     handlerRegistry.getHandlersForCommand(command);
 * </pre>
 *
 * <p>The producer uses {@link Instance} to collect all handler and metadata beans from the
 * CDI container. This approach is portable across CDI implementations and does not require
 * Quarkus-specific annotations like &#64;All.</p>
 *
 * @see HandlerRegistry
 * @see DefaultHandlerRegistry
 * @see CommandHandler
 * @see HandlerMetadata
 * @since 2.0
 */
@Singleton
public class HandlerRegistryProducer {

    private final Instance<CommandHandler<?, ?>> handlerInstances;
    private final Instance<HandlerMetadata<?, ?>> metadataInstances;

    /**
     * Constructs a new HandlerRegistryProducer with injected handler and metadata instances.
     *
     * <p>The {@link Instance} injection allows the producer to collect all beans that implement
     * {@link CommandHandler} or {@link HandlerMetadata} interfaces, regardless of their
     * specific type parameters.</p>
     *
     * @param handlerInstances all CDI-managed command handler instances; never null
     * @param metadataInstances all CDI-managed handler metadata instances; never null
     */
    @Inject
    public HandlerRegistryProducer(
            Instance<CommandHandler<?, ?>> handlerInstances,
            Instance<HandlerMetadata<?, ?>> metadataInstances) {
        this.handlerInstances = handlerInstances;
        this.metadataInstances = metadataInstances;
    }

    /**
     * Produces an application-scoped {@link HandlerRegistry} containing all discovered
     * command handlers and their metadata.
     *
     * <p>This method streams all handler and metadata instances from the CDI container
     * and constructs a {@link DefaultHandlerRegistry}. The registry is created once
     * and cached for the application lifecycle.</p>
     *
     * @return a handler registry containing all CDI-managed handlers and metadata;
     *         never null
     */
    @Produces
    @ApplicationScoped
    public HandlerRegistry produceHandlerRegistry() {
        List<CommandHandler<?, ?>> handlers = collectHandlers();
        List<HandlerMetadata<?, ?>> metadata = collectMetadata();
        return new DefaultHandlerRegistry(handlers, metadata);
    }

    /**
     * Collects all command handlers from the CDI container.
     *
     * <p>Iterates over all {@link CommandHandler} instances available in the
     * {@link Instance} collection and returns them as a list.</p>
     *
     * @return a list of all command handlers; never null, may be empty
     */
    private List<CommandHandler<?, ?>> collectHandlers() {
        List<CommandHandler<?, ?>> handlers = new ArrayList<>();
        for (CommandHandler<?, ?> handler : handlerInstances) {
            handlers.add(handler);
        }
        return handlers;
    }

    /**
     * Collects all handler metadata from the CDI container.
     *
     * <p>Iterates over all {@link HandlerMetadata} instances available in the
     * {@link Instance} collection and returns them as a list.</p>
     *
     * @return a list of all handler metadata; never null, may be empty
     */
    private List<HandlerMetadata<?, ?>> collectMetadata() {
        List<HandlerMetadata<?, ?>> metadata = new ArrayList<>();
        for (HandlerMetadata<?, ?> meta : metadataInstances) {
            metadata.add(meta);
        }
        return metadata;
    }

}
