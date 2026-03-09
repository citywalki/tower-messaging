package io.iamcyw.tower.messaging.gateway;

import io.iamcyw.tower.messaging.Command;

import java.util.concurrent.CompletableFuture;

/**
 * Gateway for sending commands to handlers.
 *
 * <p>This is the main entry point for dispatching commands in the Tower Messaging framework.
 * It provides methods for both synchronous and asynchronous command execution.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * // Synchronous execution
 * messageGateway.send(new CreateOrderCommand(customerId, items));
 *
 * // Asynchronous execution with result
 * CompletableFuture&lt;OrderResult&gt; future = messageGateway.sendAsync(
 *     new CreateOrderCommand(customerId, items),
 *     OrderResult.class
 * );
 * </pre>
 *
 * @since 2.0
 */
public interface MessageGateway {

    /**
     * Sends a command synchronously.
     *
     * <p>This method blocks until the command is processed by a handler.
     * If no handler is found for the command, an exception is thrown.</p>
     *
     * @param command the command to send; must not be null
     * @throws NullPointerException if command is null
     * @throws NoHandlerFoundException if no handler is found for the command
     * @throws CommandExecutionException if the handler throws an exception
     */
    void send(Command command);

    /**
     * Sends a command asynchronously.
     *
     * <p>This method returns immediately with a CompletableFuture that will be
     * completed when the command is processed. If no handler is found, the
     * future will be completed exceptionally with NoHandlerFoundException.</p>
     *
     * @param <R> the expected result type
     * @param command the command to send; must not be null
     * @param resultType the class of the expected result type
     * @return a CompletableFuture that will be completed with the handler's result
     * @throws NullPointerException if command or resultType is null
     */
    <R> CompletableFuture<R> sendAsync(Command command, Class<R> resultType);

    /**
     * Sends a command asynchronously without a result.
     *
     * <p>This is a convenience method for commands that don't produce a result.
     * Equivalent to {@code sendAsync(command, Void.class)}.</p>
     *
     * @param command the command to send; must not be null
     * @return a CompletableFuture that will be completed when the command is processed
     * @throws NullPointerException if command is null
     */
    default CompletableFuture<Void> sendAsync(Command command) {
        return sendAsync(command, Void.class).thenApply(r -> null);
    }

}
