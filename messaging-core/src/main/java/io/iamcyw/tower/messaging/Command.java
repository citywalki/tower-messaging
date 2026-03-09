package io.iamcyw.tower.messaging;

/**
 * Marker interface for command messages in the Tower Messaging framework.
 *
 * <p>This interface serves as a type-safe marker for all command objects that can be
 * processed by {@link CommandHandler} implementations. By implementing this interface,
 * command objects declare their intent to be handled through the command messaging
 * infrastructure.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * public record CreateOrderCommand(String customerId, List&lt;OrderItem&gt; items)
 *     implements Command {
 * }
 * </pre>
 *
 * <p>Design rationale:</p>
 * <ul>
 *   <li>Type safety: Generic constraints on {@link CommandHandler} ensure only Command
 *       implementations can be processed</li>
 *   <li>Explicit contracts: Implementing this interface makes the command nature explicit</li>
 *   <li>Extensibility: Future command-related capabilities can be added as default methods</li>
 * </ul>
 *
 * @see CommandHandler
 * @since 2.0
 */
public interface Command {
    // Marker interface - no methods required
}
