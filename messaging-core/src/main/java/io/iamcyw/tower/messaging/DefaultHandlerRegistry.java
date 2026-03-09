package io.iamcyw.tower.messaging;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link HandlerRegistry} providing type-safe handler lookup.
 *
 * <p>This implementation stores command handlers and their associated metadata,
 * enabling runtime lookup based on command type and conditional predicates.
 * It works without CDI and can be instantiated directly with lists of handlers
 * and metadata.</p>
 *
 * <p>The registry uses a two-stage filtering approach:</p>
 * <ol>
 *   <li>First, handlers are filtered by type compatibility using
 *       {@link HandlerMetadata#commandType()}.isInstance(command)</li>
 *   <li>Then, handlers are filtered by their {@link CommandHandler#canHandle(Command)}
 *       predicate</li>
 * </ol>
 *
 * <p>Example usage:</p>
 * <pre>
 * List&lt;CommandHandler&lt;?, ?&gt;&gt; handlers = List.of(
 *     new CreateOrderHandler(),
 *     new UpdateOrderHandler()
 * );
 *
 * List&lt;HandlerMetadata&lt;?, ?&gt;&gt; metadata = List.of(
 *     new CreateOrderHandler$Metadata(),
 *     new UpdateOrderHandler$Metadata()
 * );
 *
 * HandlerRegistry registry = new DefaultHandlerRegistry(handlers, metadata);
 *
 * CreateOrderCommand command = new CreateOrderCommand(customerId, items);
 * List&lt;CommandHandler&lt;CreateOrderCommand, OrderResult&gt;&gt; matchingHandlers =
 *     registry.getHandlersForCommand(command);
 * </pre>
 *
 * <p>This implementation is thread-safe for concurrent reads after construction.
 * The handler and metadata lists are captured at construction time and not modified.</p>
 *
 * @see HandlerRegistry
 * @see CommandHandler
 * @see HandlerMetadata
 * @since 2.0
 */
public class DefaultHandlerRegistry implements HandlerRegistry {

    private final List<CommandHandler<?, ?>> handlers;
    private final Map<Class<?>, HandlerMetadata<?, ?>> metadataByHandlerClass;

    /**
     * Constructs a new DefaultHandlerRegistry with the given handlers and metadata.
     *
     * <p>The handlers and metadata are captured at construction time. The metadata
     * is indexed by handler class for efficient lookup during command routing.</p>
     *
     * <p>If a handler has no corresponding metadata entry, it will be ignored during
     * lookup operations. A null metadata list will result in an empty registry.</p>
     *
     * @param handlers the list of command handlers to register; may be empty but never null
     * @param metadataList the list of handler metadata; may be empty but never null
     * @throws NullPointerException if handlers or metadataList is null
     */
    public DefaultHandlerRegistry(List<CommandHandler<?, ?>> handlers,
                                  List<HandlerMetadata<?, ?>> metadataList) {
        this.handlers = List.copyOf(Objects.requireNonNull(handlers, "handlers must not be null"));
        this.metadataByHandlerClass = Objects.requireNonNull(metadataList, "metadataList must not be null")
                .stream()
                .collect(Collectors.toUnmodifiableMap(
                        HandlerMetadata::handlerClass,
                        metadata -> metadata,
                        (existing, replacement) -> {
                            // In case of duplicate handler classes, keep the first one
                            // This handles edge cases where metadata might be registered multiple times
                            return existing;
                        }
                ));
    }

    /**
     * Returns all handlers that can process the given command.
     *
     * <p>This implementation performs the following steps:</p>
     * <ol>
     *   <li>Filters handlers whose metadata's command type is assignable from
     *       the given command's class using {@code isInstance()}</li>
     *   <li>Casts the filtered handlers to the expected generic types</li>
     *   <li>Filters handlers whose {@code canHandle()} method returns {@code true}
     *       for the given command</li>
     * </ol>
     *
     * <p>The generic cast is safe because we have already verified type compatibility
     * through the {@code isInstance()} check. The suppression is documented and
     * justified by the runtime type check.</p>
     *
     * <p>Returns an empty list if:</p>
     * <ul>
     *   <li>No handlers are registered for the command's type</li>
     *   <li>All matching handlers return {@code false} from {@code canHandle()}</li>
     *   <li>No metadata is available for registered handlers</li>
     * </ul>
     *
     * @param <C> the type of command to find handlers for; must extend {@link Command}
     * @param <R> the type of result produced by the handlers
     * @param command the command to find handlers for; never null
     * @return a list of handlers that can process the command; never null,
     *         may be empty if no matching handlers are found
     */
    @Override
    @SuppressWarnings("unchecked")
    public <C extends Command, R> List<CommandHandler<C, R>> getHandlersForCommand(C command) {
        Objects.requireNonNull(command, "command must not be null");

        return handlers.stream()
                .filter(handler -> {
                    HandlerMetadata<?, ?> metadata = metadataByHandlerClass.get(handler.getClass());
                    if (metadata == null) {
                        return false;
                    }
                    return metadata.commandType().isInstance(command);
                })
                .map(handler -> (CommandHandler<C, R>) handler)
                .filter(handler -> handler.canHandle(command))
                .collect(Collectors.toList());
    }

}
