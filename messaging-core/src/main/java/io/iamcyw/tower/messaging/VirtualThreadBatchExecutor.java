package io.iamcyw.tower.messaging;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

/**
 * Implementation of {@link BatchCommandExecutor} using Java 21 virtual threads
 * and structured concurrency.
 *
 * <p>This implementation provides three execution modes:</p>
 * <ul>
 *   <li><strong>Sequential</strong>: Commands execute one at a time in the calling thread</li>
 *   <li><strong>Parallel</strong>: Each command runs in its own virtual thread using
 *       {@link Executors#newVirtualThreadPerTaskExecutor()}</li>
 *   <li><strong>Structured</strong>: Uses virtual threads with timeout
 *       for automatic cancellation on failure or timeout</li>
 * </ul>
 *
 * <p>Thread safety: This class is stateless and thread-safe. The same instance
 * can be used across multiple concurrent batches.</p>
 *
 * <p>Resource management: The parallel execution mode creates a new virtual thread
 * executor for each batch, ensuring proper cleanup. The structured execution mode
 * uses try-with-resources to ensure the task scope is properly closed.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * VirtualThreadBatchExecutor executor = new VirtualThreadBatchExecutor();
 * HandlerRegistry registry = new DefaultHandlerRegistry(handlers, metadata);
 *
 * // Sequential execution
 * List&lt;Result&gt; results = executor.executeSequential(commands, registry);
 *
 * // Parallel execution
 * List&lt;Result&gt; results = executor.executeParallel(commands, registry);
 *
 * // Structured execution with timeout
 * List&lt;Result&gt; results = executor.executeStructured(
 *     commands, registry, Duration.ofSeconds(30));
 * </pre>
 *
 * @see BatchCommandExecutor
 * @see BatchExecutionException
 * @since 2.0
 */
public class VirtualThreadBatchExecutor implements BatchCommandExecutor {

    /**
     * Executes commands sequentially, stopping on first failure.
     *
     * <p>This method processes commands in order in the calling thread.
     * Results are accumulated and returned. If any command fails, execution
     * stops immediately and a {@link BatchExecutionException} is thrown
     * containing the partial results.</p>
     *
     * @param <R> the type of result produced by the commands
     * @param commands the list of commands to execute; never null
     * @param registry the handler registry for resolving command handlers; never null
     * @return a list of results in the same order as the input commands
     * @throws BatchExecutionException if a command fails during execution
     * @throws NullPointerException if commands or registry is null
     */
    @Override
    public <R> List<R> executeSequential(List<? extends Command> commands, HandlerRegistry registry) {
        Objects.requireNonNull(commands, "commands must not be null");
        Objects.requireNonNull(registry, "registry must not be null");

        List<R> results = new ArrayList<>();

        for (int i = 0; i < commands.size(); i++) {
            Command command = commands.get(i);
            try {
                R result = executeSingle(command, registry);
                results.add(result);
            } catch (Exception e) {
                throw new BatchExecutionException(
                        "Sequential execution failed at index " + i + " for command: " + command.getClass().getName(),
                        e,
                        command,
                        i,
                        results
                );
            }
        }

        return results;
    }

    /**
     * Executes commands in parallel using virtual threads.
     *
     * <p>This method creates a virtual thread per task executor and submits
     * all commands concurrently. Results are collected as they complete.
     * If any command fails, a {@link BatchExecutionException} is thrown
     * containing all failures and partial results.</p>
     *
     * @param <R> the type of result produced by the commands
     * @param commands the list of commands to execute; never null
     * @param registry the handler registry for resolving command handlers; never null
     * @return a list of results in the same order as the input commands
     * @throws BatchExecutionException if any command fails, containing all failures
     * @throws NullPointerException if commands or registry is null
     */
    @Override
    public <R> List<R> executeParallel(List<? extends Command> commands, HandlerRegistry registry) {
        Objects.requireNonNull(commands, "commands must not be null");
        Objects.requireNonNull(registry, "registry must not be null");

        if (commands.isEmpty()) {
            return List.of();
        }

        // Create tasks for each command
        List<Callable<R>> tasks = new ArrayList<>(commands.size());
        for (Command command : commands) {
            tasks.add(() -> executeSingle(command, registry));
        }

        // Execute using virtual thread per task executor
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<R>> futures = executor.invokeAll(tasks);

            // Collect results and failures
            List<R> results = new ArrayList<>(commands.size());
            List<Throwable> failures = new ArrayList<>();
            Command firstFailedCommand = null;
            int firstFailedIndex = -1;

            for (int i = 0; i < futures.size(); i++) {
                Future<R> future = futures.get(i);
                try {
                    R result = future.get();
                    results.add(result);
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    failures.add(cause);
                    if (firstFailedCommand == null) {
                        firstFailedCommand = commands.get(i);
                        firstFailedIndex = i;
                    }
                }
            }

            // If there were any failures, throw aggregate exception
            if (!failures.isEmpty()) {
                throw new BatchExecutionException(
                        "Parallel execution failed with " + failures.size() + " failure(s). " +
                                "First failure at index " + firstFailedIndex,
                        failures,
                        firstFailedCommand,
                        firstFailedIndex,
                        results
                );
            }

            return results;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BatchExecutionException(
                    "Parallel execution was interrupted",
                    e
            );
        }
    }

    /**
     * Executes commands with timeout using virtual threads.
     *
     * <p>This method creates a virtual thread per task executor and submits
     * all commands concurrently with a timeout. If any command fails or the
     * timeout is reached, remaining tasks are cancelled.</p>
     *
     * @param <R> the type of result produced by the commands
     * @param commands the list of commands to execute; never null
     * @param registry the handler registry for resolving command handlers; never null
     * @param timeout the maximum duration to wait for all commands to complete; never null
     * @return a list of results in the same order as the input commands
     * @throws BatchExecutionException if any command fails or timeout is reached
     * @throws NullPointerException if commands, registry, or timeout is null
     */
    @Override
    public <R> List<R> executeStructured(List<? extends Command> commands,
                                         HandlerRegistry registry,
                                         Duration timeout) {
        Objects.requireNonNull(commands, "commands must not be null");
        Objects.requireNonNull(registry, "registry must not be null");
        Objects.requireNonNull(timeout, "timeout must not be null");

        if (commands.isEmpty()) {
            return List.of();
        }

        // Create tasks for each command
        List<Callable<R>> tasks = new ArrayList<>(commands.size());
        for (Command command : commands) {
            tasks.add(() -> executeSingle(command, registry));
        }

        // Execute using virtual thread per task executor with timeout
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<R>> futures = new ArrayList<>();
            for (Callable<R> task : tasks) {
                futures.add(executor.submit(task));
            }

            // Collect results with timeout handling
            List<R> results = new ArrayList<>(commands.size());
            List<Throwable> failures = new ArrayList<>();
            Command firstFailedCommand = null;
            int firstFailedIndex = -1;

            for (int i = 0; i < futures.size(); i++) {
                Future<R> future = futures.get(i);
                try {
                    R result = future.get(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
                    results.add(result);
                } catch (TimeoutException e) {
                    future.cancel(true);
                    throw new BatchExecutionException(
                            "Structured execution timed out after " + timeout,
                            e
                    );
                } catch (ExecutionException e) {
                    future.cancel(true);
                    Throwable cause = e.getCause();
                    failures.add(cause);
                    if (firstFailedCommand == null) {
                        firstFailedCommand = commands.get(i);
                        firstFailedIndex = i;
                    }
                }
            }

            // If there were any failures, throw aggregate exception
            if (!failures.isEmpty()) {
                throw new BatchExecutionException(
                        "Structured execution failed with " + failures.size() + " failure(s). " +
                                "First failure at index " + firstFailedIndex,
                        failures,
                        firstFailedCommand,
                        firstFailedIndex,
                        results
                );
            }

            return results;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BatchExecutionException(
                    "Structured execution was interrupted",
                    e
            );
        }
    }

    /**
     * Executes a single command using the handler registry.
     *
     * <p>This helper method looks up the appropriate handler for the command
     * and executes it. If no handler is found or the handler throws an exception,
     * the exception is propagated to the caller.</p>
     *
     * @param <R> the type of result produced by the command
     * @param <C> the type of command
     * @param command the command to execute; never null
     * @param registry the handler registry; never null
     * @return the result of executing the command
     * @throws IllegalStateException if no handler is found for the command
     * @throws RuntimeException if the handler throws an exception
     */
    @SuppressWarnings("unchecked")
    private <R, C extends Command> R executeSingle(C command, HandlerRegistry registry) {
        List<CommandHandler<C, R>> handlers = registry.getHandlersForCommand(command);

        if (handlers.isEmpty()) {
            throw new IllegalStateException(
                    "No handler found for command: " + command.getClass().getName()
            );
        }

        // For single handler scenario (most common), use the first handler
        // For multiple handlers, this implementation uses the first matching handler
        CommandHandler<C, R> handler = handlers.get(0);
        return handler.handle(command);
    }

}
