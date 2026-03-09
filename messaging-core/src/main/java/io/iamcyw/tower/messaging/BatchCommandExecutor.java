package io.iamcyw.tower.messaging;

import java.time.Duration;
import java.util.List;

/**
 * Interface for executing batches of commands with different concurrency strategies.
 *
 * <p>This interface provides three execution modes for batch command processing:</p>
 * <ul>
 *   <li><strong>Sequential</strong>: Execute commands one at a time, stopping on first failure</li>
 *   <li><strong>Parallel</strong>: Execute all commands concurrently using virtual threads,
 *       collecting all results or throwing aggregate exception</li>
 *   <li><strong>Structured</strong>: Execute commands with structured concurrency, cancelling
 *       all tasks on single failure or timeout</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>
 * BatchCommandExecutor executor = new VirtualThreadBatchExecutor();
 * HandlerRegistry registry = ...;
 *
 * // Sequential execution - stops on first failure
 * List&lt;Result&gt; results = executor.executeSequential(commands, registry);
 *
 * // Parallel execution - all commands run concurrently
 * List&lt;Result&gt; results = executor.executeParallel(commands, registry);
 *
 * // Structured execution - with timeout and cancellation
 * List&lt;Result&gt; results = executor.executeStructured(
 *     commands, registry, Duration.ofSeconds(30));
 * </pre>
 *
 * <p>Thread safety: Implementations are expected to be thread-safe and stateless,
 * allowing the same executor instance to be used across multiple concurrent batches.</p>
 *
 * @see VirtualThreadBatchExecutor
 * @see BatchExecutionException
 * @since 2.0
 */
public interface BatchCommandExecutor {

    /**
     * Executes commands sequentially, stopping on first failure.
     *
     * <p>This method processes commands in the order they appear in the list.
     * If any command fails (throws an exception), execution stops immediately
     * and the exception is propagated. Successfully executed commands' results
     * are returned in a list.</p>
     *
     * <p>Use this mode when:</p>
     * <ul>
     *   <li>Commands have dependencies on previous commands' results</li>
     *   <li>Order of execution is critical</li>
     *   <li>Early failure detection is preferred over completing all commands</li>
     * </ul>
     *
     * @param <R> the type of result produced by the commands
     * @param commands the list of commands to execute; never null
     * @param registry the handler registry for resolving command handlers; never null
     * @return a list of results in the same order as the input commands;
     *         may be partial if execution stopped due to failure
     * @throws BatchExecutionException if a command fails during execution
     * @throws NullPointerException if commands or registry is null
     */
    <R> List<R> executeSequential(List<? extends Command> commands, HandlerRegistry registry);

    /**
     * Executes commands in parallel using virtual threads.
     *
     * <p>This method submits all commands to a virtual thread per task executor,
     * allowing them to run concurrently. Results are collected as they complete.
     * If any command fails, a {@link BatchExecutionException} is thrown containing
     * the partial results and information about the failures.</p>
     *
     * <p>Use this mode when:</p>
     * <ul>
     *   <li>Commands are independent and can run concurrently</li>
     *   <li>Maximum throughput is desired</li>
     *   <li>All results or complete failure information is needed</li>
     * </ul>
     *
     * @param <R> the type of result produced by the commands
     * @param commands the list of commands to execute; never null
     * @param registry the handler registry for resolving command handlers; never null
     * @return a list of results in the same order as the input commands
     * @throws BatchExecutionException if any command fails, containing partial results
     * @throws NullPointerException if commands or registry is null
     */
    <R> List<R> executeParallel(List<? extends Command> commands, HandlerRegistry registry);

    /**
     * Executes commands with structured concurrency and timeout.
     *
     * <p>This method executes all commands concurrently using virtual threads.
     * If any command fails or the timeout is reached, all remaining tasks are
     * cancelled and an exception is thrown.</p>
     *
     * <p>Use this mode when:</p>
     * <ul>
     *   <li>Commands must complete within a specific time limit</li>
     *   <li>Fast failure with cancellation is preferred over partial results</li>
     *   <li>Resource cleanup on timeout is important</li>
     * </ul>
     *
     * @param <R> the type of result produced by the commands
     * @param commands the list of commands to execute; never null
     * @param registry the handler registry for resolving command handlers; never null
     * @param timeout the maximum duration to wait for all commands to complete; never null
     * @return a list of results in the same order as the input commands
     * @throws BatchExecutionException if any command fails or timeout is reached
     * @throws NullPointerException if commands, registry, or timeout is null
     */
    <R> List<R> executeStructured(List<? extends Command> commands,
                                  HandlerRegistry registry,
                                  Duration timeout);

}
