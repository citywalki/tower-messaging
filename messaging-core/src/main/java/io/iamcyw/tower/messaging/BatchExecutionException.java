package io.iamcyw.tower.messaging;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Exception thrown when batch command execution fails.
 *
 * <p>This exception provides detailed information about batch execution failures,
 * including the command that caused the failure, partial results from successful
 * commands, and the index of the failed command in the batch.</p>
 *
 * <p>For sequential execution, this exception wraps the first failure and
 * includes results from all successfully executed commands before the failure.</p>
 *
 * <p>For parallel execution, this exception may contain multiple failures
 * and partial results from commands that completed before the failures.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * try {
 *     List&lt;Result&gt; results = executor.executeParallel(commands, registry);
 * } catch (BatchExecutionException e) {
 *     // Get the command that failed
 *     Command failedCommand = e.getFailedCommand();
 *
 *     // Get partial results
 *     List&lt;Object&gt; partialResults = e.getPartialResults();
 *
 *     // Get the index of the failed command
 *     int failedIndex = e.getFailedIndex();
 *
 *     // Get the underlying cause
 *     Throwable cause = e.getCause();
 * }
 * </pre>
 *
 * @see BatchCommandExecutor
 * @since 2.0
 */
public class BatchExecutionException extends RuntimeException {

    private final Command failedCommand;
    private final int failedIndex;
    private final List<Object> partialResults;
    private final List<Throwable> failures;

    /**
     * Creates a new batch execution exception for a single failure.
     *
     * <p>Use this constructor for sequential execution where only one
     * command can fail at a time.</p>
     *
     * @param message the error message; never null
     * @param cause the underlying cause of the failure; never null
     * @param failedCommand the command that failed; may be null if failure
     *                      occurred before command execution
     * @param failedIndex the index of the failed command in the batch
     * @param partialResults results from successfully executed commands before
     *                       the failure; never null
     * @throws NullPointerException if message, cause, or partialResults is null
     */
    public BatchExecutionException(String message,
                                   Throwable cause,
                                   Command failedCommand,
                                   int failedIndex,
                                   List<? extends Object> partialResults) {
        super(message, Objects.requireNonNull(cause, "cause must not be null"));
        this.failedCommand = failedCommand;
        this.failedIndex = failedIndex;
        this.partialResults = List.copyOf(
                Objects.requireNonNull(partialResults, "partialResults must not be null")
        );
        this.failures = List.of(cause);
    }

    /**
     * Creates a new batch execution exception for multiple failures.
     *
     * <p>Use this constructor for parallel execution where multiple
     * commands may fail concurrently.</p>
     *
     * @param message the error message; never null
     * @param failures the list of all failures that occurred; never null or empty
     * @param failedCommand the first command that failed; may be null
     * @param failedIndex the index of the first failed command
     * @param partialResults results from successfully executed commands; never null
     * @throws NullPointerException if message, failures, or partialResults is null
     * @throws IllegalArgumentException if failures is empty
     */
    public BatchExecutionException(String message,
                                   List<Throwable> failures,
                                   Command failedCommand,
                                   int failedIndex,
                                   List<? extends Object> partialResults) {
        super(message, failures.isEmpty() ? null : failures.get(0));
        Objects.requireNonNull(failures, "failures must not be null");
        if (failures.isEmpty()) {
            throw new IllegalArgumentException("failures must not be empty");
        }
        this.failedCommand = failedCommand;
        this.failedIndex = failedIndex;
        this.partialResults = List.copyOf(
                Objects.requireNonNull(partialResults, "partialResults must not be null")
        );
        this.failures = List.copyOf(failures);
    }

    /**
     * Creates a simple batch execution exception with just a message and cause.
     *
     * <p>Use this constructor for general batch failures where detailed
     * context is not available.</p>
     *
     * @param message the error message; never null
     * @param cause the underlying cause; never null
     * @throws NullPointerException if message or cause is null
     */
    public BatchExecutionException(String message, Throwable cause) {
        super(Objects.requireNonNull(message, "message must not be null"),
              Objects.requireNonNull(cause, "cause must not be null"));
        this.failedCommand = null;
        this.failedIndex = -1;
        this.partialResults = Collections.emptyList();
        this.failures = List.of(cause);
    }

    /**
     * Returns the command that caused the failure.
     *
     * <p>For parallel execution, this returns the first command that failed.
     * May be null if the failure occurred before command execution (e.g.,
     * during handler lookup).</p>
     *
     * @return the failed command, or null if not available
     */
    public Command getFailedCommand() {
        return failedCommand;
    }

    /**
     * Returns the index of the failed command in the original batch.
     *
     * <p>Returns -1 if the index is not known or applicable.</p>
     *
     * @return the index of the failed command, or -1 if not available
     */
    public int getFailedIndex() {
        return failedIndex;
    }

    /**
     * Returns the partial results from successfully executed commands.
     *
     * <p>The list contains results from commands that completed successfully
     * before the failure occurred. For sequential execution, this includes
     * all commands before the failed one. For parallel execution, this may
     * include results from commands that completed before the failure was detected.</p>
     *
     * <p>The returned list is immutable and may be empty if no commands
     * completed successfully.</p>
     *
     * @return an immutable list of partial results; never null
     */
    public List<Object> getPartialResults() {
        return partialResults;
    }

    /**
     * Returns all failures that occurred during batch execution.
     *
     * <p>For sequential execution, this list contains a single element.
     * For parallel execution, this may contain multiple failures from
     * different commands.</p>
     *
     * <p>The returned list is immutable and never empty.</p>
     *
     * @return an immutable list of all failures; never null
     */
    public List<Throwable> getFailures() {
        return failures;
    }

    /**
     * Returns the number of commands that completed successfully.
     *
     * @return the count of successful command executions
     */
    public int getSuccessCount() {
        return partialResults.size();
    }

    /**
     * Returns the number of commands that failed.
     *
     * @return the count of failed command executions
     */
    public int getFailureCount() {
        return failures.size();
    }

}
