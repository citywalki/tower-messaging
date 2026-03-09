package io.iamcyw.tower.messaging.interceptor;

import io.iamcyw.tower.messaging.Command;

/**
 * Interceptor for command processing.
 *
 * <p>Interceptors allow cross-cutting concerns to be applied to command handling,
 * such as logging, transaction management, security checks, and metrics.</p>
 *
 * <p>Interceptors are executed in a chain, with each interceptor having the option
 * to:</p>
 * <ul>
 *   <li>Pre-process the command before passing it to the next interceptor</li>
 *   <li>Post-process the result after the handler executes</li>
 *   <li>Short-circuit the chain by returning a result without calling the next interceptor</li>
 *   <li>Handle exceptions thrown by subsequent interceptors or the handler</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>
 * public class LoggingInterceptor implements CommandInterceptor {
 *     @Override
 *     public &lt;C extends Command, R&gt; R intercept(C command, CommandInterceptorChain chain) {
 *         System.out.println("Before: " + command);
 *         R result = chain.proceed(command);
 *         System.out.println("After: " + result);
 *         return result;
 *     }
 * }
 * </pre>
 *
 * @see CommandInterceptorChain
 * @since 2.0
 */
public interface CommandInterceptor {

    /**
     * Returns the order of this interceptor.
     *
     * <p>Interceptors with lower order values are executed first. The default order is 0.</p>
     *
     * @return the order value
     */
    default int order() {
        return 0;
    }

    /**
     * Intercepts command execution.
     *
     * <p>This method is called for each command that passes through the interceptor chain.
     * Implementations should typically call {@code chain.proceed(command)} to continue
     * to the next interceptor or the handler, but may also short-circuit by returning
     * a result directly.</p>
     *
     * @param <C> the command type
     * @param <R> the result type
     * @param command the command being processed; never null
     * @param chain the interceptor chain for proceeding to the next interceptor
     * @return the result of command processing
     * @throws Exception if processing fails
     */
    <C extends Command, R> R intercept(C command, CommandInterceptorChain chain) throws Exception;

}
