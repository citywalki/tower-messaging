package io.iamcyw.tower.messaging;

/**
 * Core interface for command handlers in the Tower Messaging framework.
 *
 * <p>Implementations of this interface process commands of type {@code C} and produce
 * results of type {@code R}. The interface enforces compile-time type safety through
 * its generic parameters, ensuring that handlers can only process compatible command
 * types.</p>
 *
 * <p>Example implementation:</p>
 * <pre>
 * @Singleton
 * public class CreateOrderHandler implements CommandHandler&lt;CreateOrderCommand, OrderResult&gt; {
 *
 *     private final OrderRepository repository;
 *
 *     @Inject
 *     public CreateOrderHandler(OrderRepository repository) {
 *         this.repository = repository;
 *     }
 *
 *     @Override
 *     public OrderResult handle(CreateOrderCommand command) {
 *         Order order = new Order(command.customerId(), command.items());
 *         repository.save(order);
 *         return new OrderResult(order.getId());
 *     }
 *
 *     @Override
 *     public boolean canHandle(CreateOrderCommand command) {
 *         return command.items() != null &amp;&amp; !command.items().isEmpty();
 *     }
 * }
 * </pre>
 *
 * <p>Conditional routing:</p>
 * The {@link #canHandle(Command)} method allows multiple handlers to be registered
 * for the same command type with different routing conditions. This enables scenarios
 * such as:
 * <ul>
 *   <li>VIP customer orders handled by a priority handler</li>
 *   <li>Orders by region routed to region-specific handlers</li>
 *   <li>Commands filtered by validation state</li>
 * </ul>
 *
 * @param <C> the type of command this handler can process; must extend {@link Command}
 * @param <R> the type of result this handler produces
 *
 * @see Command
 * @see HandlerMetadata
 * @since 2.0
 */
public interface CommandHandler<C extends Command, R> {

    /**
     * Processes the given command and returns a result.
     *
     * <p>This method contains the core business logic for handling the command.
     * Implementations should perform their work and return the appropriate result.
     * If the command cannot be processed, implementations should throw a runtime
     * exception describing the failure.</p>
     *
     * @param command the command to process; never null
     * @return the result of processing the command; may be null if the handler
     *         produces no result
     * @throws RuntimeException if the command cannot be processed
     */
    R handle(C command);

    /**
     * Determines whether this handler can process the given command.
     *
     * <p>This default implementation returns {@code true} for all commands,
     * meaning the handler accepts any command of its declared type. Subclasses
     * may override this method to implement conditional routing logic.</p>
     *
     * <p>The {@code canHandle} check is performed after type compatibility is
     * verified. It allows fine-grained control over which handler instance
     * processes a command based on the command's content.</p>
     *
     * @param command the command to evaluate; never null
     * @return {@code true} if this handler can process the command,
     *         {@code false} otherwise
     */
    default boolean canHandle(C command) {
        return true;
    }

}
