package io.iamcyw.tower.quarkus.runtime;

import io.iamcyw.tower.messaging.Command;
import io.iamcyw.tower.messaging.CommandHandler;
import io.iamcyw.tower.messaging.DefaultHandlerRegistry;
import io.iamcyw.tower.messaging.HandlerMetadata;
import io.iamcyw.tower.messaging.HandlerRegistry;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.inject.Singleton;
import org.jboss.logging.Logger;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * CDI-managed implementation of {@link HandlerRegistry} for Quarkus runtime.
 *
 * <p>This registry integrates with Quarkus CDI to automatically collect all
 * {@link CommandHandler} implementations and their associated {@link HandlerMetadata}
 * at runtime. It delegates the actual lookup logic to {@link DefaultHandlerRegistry}.</p>
 *
 * <p>The registry uses Quarkus ArC's programmatic API to collect all handler and metadata
 * beans, which are produced by the build-time processor {@code MessageQuarkusProcessor}.
 * This enables zero-reflection handler discovery and type-safe routing.</p>
 *
 * <p>Usage example:</p>
 * <pre>
 * &#64;Inject
 * HandlerRegistry registry;
 *
 * public void processCommand(CreateOrderCommand command) {
 *     List&lt;CommandHandler&lt;CreateOrderCommand, OrderResult&gt;&gt; handlers =
 *         registry.getHandlersForCommand(command);
 *
 *     if (handlers.isEmpty()) {
 *         throw new NoHandlerFoundException(command);
 *     }
 *
 *     // Execute the first matching handler
 *     OrderResult result = handlers.get(0).handle(command);
 * }
 * </pre>
 *
 * <p><strong>CDI Integration Note:</strong> This class uses Quarkus ArC's programmatic
 * API ({@link Arc#container()}) to look up handlers and metadata instead of
 * {@code Instance<CommandHandler<?, ?>>} injection. This is necessary because CDI's
 * type-safe resolution does not match wildcard types ({@code CommandHandler<?, ?>})
 * with concrete parameterized types ({@code CommandHandler<MyCommand, MyResult>}).
 * See CDI specification section 5.2.3 for details.</p>
 *
 * <p>This class is annotated with {@link Alternative} and {@link Priority} to override
 * the default {@link HandlerRegistry} producer from the messaging-cdi module.</p>
 *
 * <p>Thread safety: This registry is thread-safe as it delegates to
 * {@link DefaultHandlerRegistry} which is immutable after construction.</p>
 *
 * @see HandlerRegistry
 * @see DefaultHandlerRegistry
 * @see CommandHandler
 * @see HandlerMetadata
 * @see Arc
 * @since 2.0
 */
@Singleton
@Alternative
@Priority(1)
public class QuarkusHandlerRegistry implements HandlerRegistry {

    private static final Logger LOG = Logger.getLogger(QuarkusHandlerRegistry.class);

    private volatile DefaultHandlerRegistry delegate;

    /**
     * Constructs a new QuarkusHandlerRegistry.
     *
     * <p>The actual initialization of handlers and metadata is deferred to the first
     * call to {@link #getHandlersForCommand(Command)} using lazy initialization.
     * This is necessary because when this singleton bean is constructed, the CDI
     * container may not have all beans fully initialized yet.</p>
     */
    public QuarkusHandlerRegistry() {
        // Defer initialization to first use
    }

    /**
     * Collects all command handlers from the CDI container using ArC's programmatic API.
     *
     * <p>This method uses {@link Arc#container()} to obtain the container and then
     * uses {@code listAll()} to get all beans implementing {@code CommandHandler}.
     * This approach works around the CDI limitation where {@code Instance<CommandHandler<?, ?>>}
     * cannot match beans with concrete type parameters.</p>
     *
     * @return a list of all command handlers; never null, may be empty
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<CommandHandler<?, ?>> collectHandlers() {
        List<CommandHandler<?, ?>> handlers = new ArrayList<>();

        // Use raw type to avoid generic type inference issues with listAll
        List handles = Arc.container()
                .listAll(CommandHandler.class, jakarta.enterprise.inject.Any.Literal.INSTANCE);

        LOG.debugf("collectHandlers: found %d handles for CommandHandler.class", handles.size());

        // If listAll returns empty, iterate through all beans and filter manually
        // This is necessary because parameterized types like CommandHandler<C, R>
        // may not match the raw type CommandHandler.class in listAll
        if (handles.isEmpty()) {
            LOG.debugf("Falling back to manual bean discovery for CommandHandlers");
            Set<Bean<?>> beans = Arc.container().beanManager().getBeans(Object.class,
                    jakarta.enterprise.inject.Any.Literal.INSTANCE);
            for (Bean<?> bean : beans) {
                Set<Type> types = bean.getTypes();
                for (Type type : types) {
                    if (type instanceof ParameterizedType) {
                        ParameterizedType pt = (ParameterizedType) type;
                        if (pt.getRawType().equals(CommandHandler.class)) {
                            LOG.debugf("  - Found handler bean: %s", bean.getBeanClass());
                            // Use the bean's bean class to get an instance
                            InstanceHandle handle = Arc.container().instance(bean.getBeanClass());
                            if (handle.isAvailable()) {
                                handlers.add((CommandHandler<?, ?>) handle.get());
                            }
                            break;
                        }
                    }
                }
            }
            return handlers;
        }

        for (Object handle : handles) {
            CommandHandler<?, ?> handler = ((InstanceHandle<CommandHandler<?, ?>>) handle).get();
            LOG.infof("  - Handler: %s", handler.getClass().getName());
            handlers.add(handler);
        }
        return handlers;
    }

    /**
     * Collects all handler metadata from the CDI container using ArC's programmatic API.
     *
     * <p>This method uses {@link Arc#container()} to obtain the container and then
     * uses {@code listAll()} to get all beans implementing {@code HandlerMetadata}.
     * The metadata beans are generated at build time by {@code HandlerMetadataGenerator}.</p>
     *
     * @return a list of all handler metadata; never null, may be empty
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<HandlerMetadata<?, ?>> collectMetadata() {
        List<HandlerMetadata<?, ?>> metadata = new ArrayList<>();

        // Use raw type to avoid generic type inference issues with listAll
        List handles = Arc.container()
                .listAll(HandlerMetadata.class, jakarta.enterprise.inject.Any.Literal.INSTANCE);

        LOG.debugf("collectMetadata: found %d handles for HandlerMetadata.class", handles.size());

        // If listAll returns empty, iterate through all beans and filter manually
        if (handles.isEmpty()) {
            LOG.debugf("Falling back to manual bean discovery for HandlerMetadata");
            Set<Bean<?>> allBeans = Arc.container().beanManager().getBeans(Object.class,
                    jakarta.enterprise.inject.Any.Literal.INSTANCE);
            for (Bean<?> bean : allBeans) {
                Set<Type> types = bean.getTypes();
                for (Type type : types) {
                    if (type instanceof ParameterizedType) {
                        ParameterizedType pt = (ParameterizedType) type;
                        if (pt.getRawType().equals(HandlerMetadata.class)) {
                            LOG.debugf("  - Found metadata bean: %s", bean.getBeanClass());
                            InstanceHandle handle = Arc.container().instance(bean.getBeanClass());
                            if (handle.isAvailable()) {
                                metadata.add((HandlerMetadata<?, ?>) handle.get());
                            }
                            break;
                        }
                    }
                }
            }
            return metadata;
        }

        for (Object handle : handles) {
            metadata.add(((InstanceHandle<HandlerMetadata<?, ?>>) handle).get());
        }
        return metadata;
    }

    /**
     * Returns all handlers that can process the given command.
     *
     * <p>This method uses lazy initialization to collect handlers on the first call.
     * This is necessary because the CDI container may not be fully initialized when
     * this singleton bean is constructed. Subsequent calls use the cached registry.</p>
     *
     * <p>The actual lookup is delegated to {@link DefaultHandlerRegistry#getHandlersForCommand(Command)}
     * which performs type-based filtering followed by condition-based filtering using
     * {@link CommandHandler#canHandle(Command)}.</p>
     *
     * @param <C>     the type of command to find handlers for; must extend {@link Command}
     * @param <R>     the type of result produced by the handlers
     * @param command the command to find handlers for; never null
     * @return a list of handlers that can process the command; never null,
     *         may be empty if no matching handlers are found
     */
    @Override
    public <C extends Command, R> List<CommandHandler<C, R>> getHandlersForCommand(C command) {
        // Lazy initialization - first call initializes the registry
        if (delegate == null) {
            synchronized (this) {
                if (delegate == null) {
                    initialize();
                }
            }
        }
        return delegate.getHandlersForCommand(command);
    }

    /**
     * Initializes the registry by collecting all handlers and metadata from CDI.
     *
     * <p>This method is called lazily on the first access to ensure the CDI container
     * is fully initialized before we try to look up beans.</p>
     *
     * <p>If no metadata beans are found (e.g., when generated metadata classes are not
     * properly registered with CDI), this method falls back to creating
     * {@link ReflectiveHandlerMetadata} instances that extract type information from
     * the handlers using reflection.</p>
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void initialize() {
        List<CommandHandler<?, ?>> handlers = collectHandlers();
        List<HandlerMetadata<?, ?>> metadataList = collectMetadata();

        // If no metadata beans were found, create reflective metadata from handlers
        if (metadataList.isEmpty() && !handlers.isEmpty()) {
            LOG.debugf("No metadata beans found, creating reflective metadata from handlers");
            for (CommandHandler<?, ?> handler : handlers) {
                try {
                    HandlerMetadata<?, ?> metadata = new ReflectiveHandlerMetadata(handler);
                    metadataList.add(metadata);
                    LOG.debugf("  - Created metadata for: %s -> command=%s, result=%s",
                              handler.getClass().getName(),
                              metadata.commandType().getName(),
                              metadata.resultType() != null ? metadata.resultType().getName() : "void");
                } catch (Exception e) {
                    LOG.warnf("Failed to create metadata for handler %s: %s",
                              handler.getClass().getName(), e.getMessage());
                }
            }
        }

        LOG.debugf("Initializing QuarkusHandlerRegistry with %d handlers and %d metadata entries",
                  handlers.size(), metadataList.size());

        this.delegate = new DefaultHandlerRegistry(handlers, metadataList);

        handlers.forEach(h -> LOG.debugf("Registered handler: %s", h.getClass().getName()));
    }

}
