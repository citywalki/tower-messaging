package io.iamcyw.tower.messaging.interceptor;

import io.iamcyw.tower.messaging.Command;

/**
 * Chain of interceptors for command processing.
 *
 * <p>The interceptor chain represents the remaining interceptors and the final handler
 * that will process a command. Each interceptor receives the chain and can choose to:</p>
 *
 * <ul>
 *   <li>Call {@link #proceed(Command)} to continue to the next interceptor or handler</li>
 *   <li>Return a value directly without proceeding</li>
 *   <li>Throw an exception to abort processing</li>
 * </ul>
 *
 * <p>Example implementation in an interceptor:</p>
 * <pre>
 * @Override
 * public &lt;C extends Command, R&gt; R intercept(C command, CommandInterceptorChain chain) {
 *     // Pre-processing
 *     validateCommand(command);
 *
 *     // Proceed to next interceptor or handler
 *     R result = chain.proceed(command);
 *
 *     // Post-processing
 *     auditResult(result);
 *
 *     return result;
 * }
 * </pre>
 *
 * @see CommandInterceptor
 * @since 2.0
 */
public interface CommandInterceptorChain {

    /**
     * Proceeds to the next interceptor in the chain or executes the handler.
     *
     * <p>This method passes control to the next interceptor in the chain. If this
     * is the last interceptor, the handler will be invoked.</p>
     *
     * @param <C> the command type
     * @param <R> the result type
     * @param command the command to process; never null
     * @return the result from the next interceptor or handler
     * @throws Exception if processing fails
     */
    <C extends Command, R> R proceed(C command) throws Exception;

}
