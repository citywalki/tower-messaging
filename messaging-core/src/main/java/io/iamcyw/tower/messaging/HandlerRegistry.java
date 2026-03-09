package io.iamcyw.tower.messaging;

import java.util.List;

/**
 * Registry for command handlers providing type-safe lookup capabilities.
 *
 * <p>This interface defines the contract for retrieving command handlers that can
 * process a given command. Implementations are responsible for filtering handlers
 * based on both type compatibility and runtime conditions (via {@link CommandHandler#canHandle(Command)}).</p>
 *
 * <p>The registry supports multiple handlers for the same command type, enabling
 * conditional routing scenarios where different handlers process commands based on
 * their content. For example, VIP orders might be routed to a priority handler while
 * regular orders go to a standard handler.</p>
 *
 * <p>Usage example:</p>
 * <pre>
 * HandlerRegistry registry = new DefaultHandlerRegistry(handlers, metadata);
 *
 * CreateOrderCommand command = new CreateOrderCommand(customerId, items);
 * List&lt;CommandHandler&lt;CreateOrderCommand, OrderResult&gt;&gt; handlers =
 *     registry.getHandlersForCommand(command);
 *
 * if (handlers.isEmpty()) {
 *     throw new NoHandlerFoundException(command);
 * }
 *
 * // For single handler scenario
 * OrderResult result = handlers.get(0).handle(command);
 * </pre>
 *
 * <p>Implementations should filter handlers in the following order:</p>
 * <ol>
 *   <li>Filter by type compatibility using {@link HandlerMetadata#commandType()}</li>
 *   <li>Filter by {@link CommandHandler#canHandle(Command)} predicate</li>
 * </ol>
 *
 * <p>Thread safety: Implementations are expected to be thread-safe as handlers are
 * typically registered at startup and the registry is accessed concurrently during
 * message processing.</p>
 *
 * @see CommandHandler
 * @see HandlerMetadata
 * @see DefaultHandlerRegistry
 * @since 2.0
 */
public interface HandlerRegistry {

    /**
     * Returns all handlers that can process the given command.
     *
     * <p>This method performs a two-stage filtering process:</p>
     * <ol>
     *   <li>Type filtering: Only handlers whose declared command type is assignable
     *       from the given command's class are considered</li>
     *   <li>Condition filtering: Only handlers whose {@code canHandle()} method
     *       returns {@code true} for the given command are returned</li>
     * </ol>
     *
     * <p>If no handlers match, an empty list is returned. Callers should handle
     * this case appropriately, typically by throwing an exception or returning
     * a default result.</p>
     *
     * <p>Multiple handlers may be returned when conditional routing is used.
     * The caller is responsible for selecting the appropriate handler or
     * executing multiple handlers if the use case requires it.</p>
     *
     * @param <C> the type of command to find handlers for; must extend {@link Command}
     * @param <R> the type of result produced by the handlers
     * @param command the command to find handlers for; never null
     * @return a list of handlers that can process the command; never null,
     *         may be empty if no matching handlers are found
     */
    <C extends Command, R> List<CommandHandler<C, R>> getHandlersForCommand(C command);

}
