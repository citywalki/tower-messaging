package io.iamcyw.tower.messaging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link VirtualThreadBatchExecutor}.
 *
 * <p>These tests verify the three execution modes (sequential, parallel, structured)
 * and their error handling behaviors.</p>
 */
class VirtualThreadBatchExecutorTest {

    /**
     * Test command for batch execution tests.
     */
    record TestCommand(String value) implements Command {
    }

    /**
     * Test result type.
     */
    record TestResult(String value) {
    }

    /**
     * Command that simulates slow execution for timeout testing.
     */
    record SlowCommand(long sleepMillis) implements Command {
    }

    /**
     * Command that always fails when executed.
     */
    record FailingCommand(String errorMessage) implements Command {
    }

    /**
     * Simple handler that processes TestCommand.
     */
    static class TestCommandHandler implements CommandHandler<TestCommand, TestResult> {

        private final AtomicInteger callCount = new AtomicInteger(0);

        @Override
        public TestResult handle(TestCommand command) {
            callCount.incrementAndGet();
            return new TestResult("handled:" + command.value());
        }

        public int getCallCount() {
            return callCount.get();
        }
    }

    /**
     * Handler for slow commands that simulates delay.
     */
    static class SlowCommandHandler implements CommandHandler<SlowCommand, TestResult> {

        @Override
        public TestResult handle(SlowCommand command) {
            try {
                Thread.sleep(command.sleepMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted", e);
            }
            return new TestResult("slow:" + command.sleepMillis());
        }
    }

    /**
     * Handler that fails for FailingCommand.
     */
    static class FailingCommandHandler implements CommandHandler<FailingCommand, TestResult> {

        @Override
        public TestResult handle(FailingCommand command) {
            throw new RuntimeException(command.errorMessage());
        }
    }

    /**
     * Creates a simple metadata instance for testing.
     */
    @SuppressWarnings("unchecked")
    private static <C extends Command, R> HandlerMetadata<C, R> createMetadata(
            String handlerName,
            Class<C> commandType,
            Class<R> resultType,
            Class<? extends CommandHandler<C, R>> handlerClass) {
        return new HandlerMetadata<>() {
            @Override
            public Class<C> commandType() {
                return commandType;
            }

            @Override
            public Class<R> resultType() {
                return resultType;
            }

            @Override
            public String handlerName() {
                return handlerName;
            }

            @Override
            public Class<? extends CommandHandler<C, R>> handlerClass() {
                return handlerClass;
            }
        };
    }

    @Test
    @DisplayName("executeSequential should process commands in order")
    void sequentialShouldProcessCommandsInOrder() {
        // Given - executor with handler
        VirtualThreadBatchExecutor executor = new VirtualThreadBatchExecutor();
        TestCommandHandler handler = new TestCommandHandler();

        HandlerMetadata<TestCommand, TestResult> metadata = createMetadata(
                handler.getClass().getName(),
                TestCommand.class,
                TestResult.class,
                TestCommandHandler.class
        );

        HandlerRegistry registry = new DefaultHandlerRegistry(
                List.of(handler),
                List.of(metadata)
        );

        List<TestCommand> commands = List.of(
                new TestCommand("first"),
                new TestCommand("second"),
                new TestCommand("third")
        );

        // When - execute sequentially
        List<TestResult> results = executor.executeSequential(commands, registry);

        // Then - results should be in order
        assertThat(results).hasSize(3);
        assertThat(results.get(0).value()).isEqualTo("handled:first");
        assertThat(results.get(1).value()).isEqualTo("handled:second");
        assertThat(results.get(2).value()).isEqualTo("handled:third");
        assertThat(handler.getCallCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("executeSequential should stop on first failure")
    void sequentialShouldStopOnFirstFailure() {
        // Given - executor with handlers
        VirtualThreadBatchExecutor executor = new VirtualThreadBatchExecutor();
        TestCommandHandler successHandler = new TestCommandHandler();
        FailingCommandHandler failingHandler = new FailingCommandHandler();

        HandlerMetadata<TestCommand, TestResult> successMetadata = createMetadata(
                successHandler.getClass().getName(),
                TestCommand.class,
                TestResult.class,
                TestCommandHandler.class
        );
        HandlerMetadata<FailingCommand, TestResult> failingMetadata = createMetadata(
                failingHandler.getClass().getName(),
                FailingCommand.class,
                TestResult.class,
                FailingCommandHandler.class
        );

        HandlerRegistry registry = new DefaultHandlerRegistry(
                List.of(successHandler, failingHandler),
                List.of(successMetadata, failingMetadata)
        );

        List<Command> commands = List.of(
                new TestCommand("first"),
                new FailingCommand("expected error"),
                new TestCommand("third")
        );

        // When/Then - should throw on failure with partial results
        assertThatThrownBy(() -> executor.executeSequential(commands, registry))
                .isInstanceOf(BatchExecutionException.class)
                .satisfies(e -> {
                    BatchExecutionException be = (BatchExecutionException) e;
                    assertThat(be.getFailedIndex()).isEqualTo(1);
                    assertThat(be.getFailedCommand()).isInstanceOf(FailingCommand.class);
                    assertThat(be.getPartialResults()).hasSize(1);
                    assertThat(be.getCause()).hasMessageContaining("expected error");
                });

        // Should have processed only first command
        assertThat(successHandler.getCallCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("executeSequential should return empty list for empty commands")
    void sequentialShouldReturnEmptyListForEmptyCommands() {
        // Given
        VirtualThreadBatchExecutor executor = new VirtualThreadBatchExecutor();
        HandlerRegistry registry = new DefaultHandlerRegistry(
                Collections.emptyList(),
                Collections.emptyList()
        );

        // When
        List<TestResult> results = executor.executeSequential(Collections.emptyList(), registry);

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("executeSequential should throw NullPointerException for null commands")
    void sequentialShouldThrowForNullCommands() {
        VirtualThreadBatchExecutor executor = new VirtualThreadBatchExecutor();
        HandlerRegistry registry = new DefaultHandlerRegistry(
                Collections.emptyList(),
                Collections.emptyList()
        );

        assertThatThrownBy(() -> executor.executeSequential(null, registry))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("commands");
    }

    @Test
    @DisplayName("executeSequential should throw NullPointerException for null registry")
    void sequentialShouldThrowForNullRegistry() {
        VirtualThreadBatchExecutor executor = new VirtualThreadBatchExecutor();
        List<TestCommand> commands = List.of(new TestCommand("test"));

        assertThatThrownBy(() -> executor.executeSequential(commands, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("registry");
    }

    @Test
    @DisplayName("executeParallel should process all commands concurrently")
    void parallelShouldProcessAllCommands() {
        // Given - executor with handler
        VirtualThreadBatchExecutor executor = new VirtualThreadBatchExecutor();
        TestCommandHandler handler = new TestCommandHandler();

        HandlerMetadata<TestCommand, TestResult> metadata = createMetadata(
                handler.getClass().getName(),
                TestCommand.class,
                TestResult.class,
                TestCommandHandler.class
        );

        HandlerRegistry registry = new DefaultHandlerRegistry(
                List.of(handler),
                List.of(metadata)
        );

        List<TestCommand> commands = List.of(
                new TestCommand("first"),
                new TestCommand("second"),
                new TestCommand("third")
        );

        // When - execute in parallel
        List<TestResult> results = executor.executeParallel(commands, registry);

        // Then - all results should be present (order may vary in parallel execution)
        assertThat(results).hasSize(3);
        assertThat(results)
                .extracting(TestResult::value)
                .containsExactlyInAnyOrder("handled:first", "handled:second", "handled:third");
    }

    @Test
    @DisplayName("executeParallel should collect all failures and partial results")
    void parallelShouldCollectAllFailures() {
        // Given - executor with failing handler
        VirtualThreadBatchExecutor executor = new VirtualThreadBatchExecutor();
        TestCommandHandler successHandler = new TestCommandHandler();
        FailingCommandHandler failingHandler = new FailingCommandHandler();

        HandlerMetadata<TestCommand, TestResult> successMetadata = createMetadata(
                successHandler.getClass().getName(),
                TestCommand.class,
                TestResult.class,
                TestCommandHandler.class
        );
        HandlerMetadata<FailingCommand, TestResult> failingMetadata = createMetadata(
                failingHandler.getClass().getName(),
                FailingCommand.class,
                TestResult.class,
                FailingCommandHandler.class
        );

        HandlerRegistry registry = new DefaultHandlerRegistry(
                List.of(successHandler, failingHandler),
                List.of(successMetadata, failingMetadata)
        );

        List<Command> commands = List.of(
                new FailingCommand("error1"),
                new TestCommand("success"),
                new FailingCommand("error2")
        );

        // When/Then - should throw aggregate exception
        assertThatThrownBy(() -> executor.executeParallel(commands, registry))
                .isInstanceOf(BatchExecutionException.class)
                .satisfies(e -> {
                    BatchExecutionException be = (BatchExecutionException) e;
                    assertThat(be.getFailureCount()).isEqualTo(2);
                    assertThat(be.getPartialResults()).hasSize(1);
                    assertThat(be.getFailures()).hasSize(2);
                });
    }

    @Test
    @DisplayName("executeParallel should return empty list for empty commands")
    void parallelShouldReturnEmptyListForEmptyCommands() {
        VirtualThreadBatchExecutor executor = new VirtualThreadBatchExecutor();
        HandlerRegistry registry = new DefaultHandlerRegistry(
                Collections.emptyList(),
                Collections.emptyList()
        );

        List<TestResult> results = executor.executeParallel(Collections.emptyList(), registry);

        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("executeParallel should throw NullPointerException for null commands")
    void parallelShouldThrowForNullCommands() {
        VirtualThreadBatchExecutor executor = new VirtualThreadBatchExecutor();
        HandlerRegistry registry = new DefaultHandlerRegistry(
                Collections.emptyList(),
                Collections.emptyList()
        );

        assertThatThrownBy(() -> executor.executeParallel(null, registry))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("commands");
    }

    @Test
    @DisplayName("executeParallel should throw NullPointerException for null registry")
    void parallelShouldThrowForNullRegistry() {
        VirtualThreadBatchExecutor executor = new VirtualThreadBatchExecutor();
        List<TestCommand> commands = List.of(new TestCommand("test"));

        assertThatThrownBy(() -> executor.executeParallel(commands, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("registry");
    }

    @Test
    @DisplayName("executeStructured should process all commands with timeout")
    void structuredShouldProcessAllCommands() {
        // Given - executor with handler
        VirtualThreadBatchExecutor executor = new VirtualThreadBatchExecutor();
        TestCommandHandler handler = new TestCommandHandler();

        HandlerMetadata<TestCommand, TestResult> metadata = createMetadata(
                handler.getClass().getName(),
                TestCommand.class,
                TestResult.class,
                TestCommandHandler.class
        );

        HandlerRegistry registry = new DefaultHandlerRegistry(
                List.of(handler),
                List.of(metadata)
        );

        List<TestCommand> commands = List.of(
                new TestCommand("first"),
                new TestCommand("second"),
                new TestCommand("third")
        );

        // When - execute with structured concurrency
        List<TestResult> results = executor.executeStructured(
                commands, registry, Duration.ofSeconds(10));

        // Then
        assertThat(results).hasSize(3);
    }

    @Test
    @DisplayName("executeStructured should cancel all tasks on failure")
    void structuredShouldCancelOnFailure() {
        // Given - executor with failing handler
        VirtualThreadBatchExecutor executor = new VirtualThreadBatchExecutor();
        SlowCommandHandler slowHandler = new SlowCommandHandler();
        FailingCommandHandler failingHandler = new FailingCommandHandler();

        HandlerMetadata<SlowCommand, TestResult> slowMetadata = createMetadata(
                slowHandler.getClass().getName(),
                SlowCommand.class,
                TestResult.class,
                SlowCommandHandler.class
        );
        HandlerMetadata<FailingCommand, TestResult> failingMetadata = createMetadata(
                failingHandler.getClass().getName(),
                FailingCommand.class,
                TestResult.class,
                FailingCommandHandler.class
        );

        HandlerRegistry registry = new DefaultHandlerRegistry(
                List.of(slowHandler, failingHandler),
                List.of(slowMetadata, failingMetadata)
        );

        List<Command> commands = List.of(
                new SlowCommand(5000),  // Will be cancelled
                new FailingCommand("immediate failure"),
                new SlowCommand(5000)   // Will be cancelled
        );

        // When/Then - should fail immediately when failing command throws
        assertThatThrownBy(() ->
                executor.executeStructured(commands, registry, Duration.ofSeconds(30)))
                .isInstanceOf(BatchExecutionException.class)
                .satisfies(e -> {
                    BatchExecutionException be = (BatchExecutionException) e;
                    assertThat(be.getCause()).hasMessageContaining("immediate failure");
                });
    }

    @Test
    @DisplayName("executeStructured should timeout and cancel tasks")
    void structuredShouldTimeout() {
        // Given - executor with slow handler
        VirtualThreadBatchExecutor executor = new VirtualThreadBatchExecutor();
        SlowCommandHandler handler = new SlowCommandHandler();

        HandlerMetadata<SlowCommand, TestResult> metadata = createMetadata(
                handler.getClass().getName(),
                SlowCommand.class,
                TestResult.class,
                SlowCommandHandler.class
        );

        HandlerRegistry registry = new DefaultHandlerRegistry(
                List.of(handler),
                List.of(metadata)
        );

        List<SlowCommand> commands = List.of(
                new SlowCommand(5000),
                new SlowCommand(5000)
        );

        // When/Then - should timeout
        assertThatThrownBy(() ->
                executor.executeStructured(commands, registry, Duration.ofMillis(100)))
                .isInstanceOf(BatchExecutionException.class)
                .satisfies(e -> {
                    BatchExecutionException be = (BatchExecutionException) e;
                    assertThat(be.getCause()).isInstanceOf(java.util.concurrent.TimeoutException.class);
                });
    }

    @Test
    @DisplayName("executeStructured should return empty list for empty commands")
    void structuredShouldReturnEmptyListForEmptyCommands() {
        VirtualThreadBatchExecutor executor = new VirtualThreadBatchExecutor();
        HandlerRegistry registry = new DefaultHandlerRegistry(
                Collections.emptyList(),
                Collections.emptyList()
        );

        List<TestResult> results = executor.executeStructured(
                Collections.emptyList(), registry, Duration.ofSeconds(1));

        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("executeStructured should throw NullPointerException for null commands")
    void structuredShouldThrowForNullCommands() {
        VirtualThreadBatchExecutor executor = new VirtualThreadBatchExecutor();
        HandlerRegistry registry = new DefaultHandlerRegistry(
                Collections.emptyList(),
                Collections.emptyList()
        );

        assertThatThrownBy(() ->
                executor.executeStructured(null, registry, Duration.ofSeconds(1)))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("commands");
    }

    @Test
    @DisplayName("executeStructured should throw NullPointerException for null registry")
    void structuredShouldThrowForNullRegistry() {
        VirtualThreadBatchExecutor executor = new VirtualThreadBatchExecutor();
        List<TestCommand> commands = List.of(new TestCommand("test"));

        assertThatThrownBy(() ->
                executor.executeStructured(commands, null, Duration.ofSeconds(1)))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("registry");
    }

    @Test
    @DisplayName("executeStructured should throw NullPointerException for null timeout")
    void structuredShouldThrowForNullTimeout() {
        VirtualThreadBatchExecutor executor = new VirtualThreadBatchExecutor();
        TestCommandHandler handler = new TestCommandHandler();

        HandlerMetadata<TestCommand, TestResult> metadata = createMetadata(
                handler.getClass().getName(),
                TestCommand.class,
                TestResult.class,
                TestCommandHandler.class
        );

        HandlerRegistry registry = new DefaultHandlerRegistry(
                List.of(handler),
                List.of(metadata)
        );

        List<TestCommand> commands = List.of(new TestCommand("test"));

        assertThatThrownBy(() -> executor.executeStructured(commands, registry, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("timeout");
    }

    @Test
    @DisplayName("BatchExecutionException should store failed command and partial results")
    void batchExecutionExceptionShouldStoreContext() {
        // Given
        TestCommand failedCommand = new TestCommand("failed");
        List<String> partialResults = List.of("result1", "result2");
        RuntimeException cause = new RuntimeException("test error");

        // When
        BatchExecutionException exception = new BatchExecutionException(
                "Test failure",
                cause,
                failedCommand,
                2,
                partialResults
        );

        // Then
        assertThat(exception.getFailedCommand()).isEqualTo(failedCommand);
        assertThat(exception.getFailedIndex()).isEqualTo(2);
        assertThat(exception.getPartialResults()).hasSize(2);
        assertThat(exception.getSuccessCount()).isEqualTo(2);
        assertThat(exception.getFailureCount()).isEqualTo(1);
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    @DisplayName("BatchExecutionException should support multiple failures")
    void batchExecutionExceptionShouldSupportMultipleFailures() {
        // Given
        List<Throwable> failures = List.of(
                new RuntimeException("error1"),
                new RuntimeException("error2")
        );
        TestCommand failedCommand = new TestCommand("failed");
        List<String> partialResults = List.of("result1");

        // When
        BatchExecutionException exception = new BatchExecutionException(
                "Multiple failures",
                failures,
                failedCommand,
                1,
                partialResults
        );

        // Then
        assertThat(exception.getFailures()).hasSize(2);
        assertThat(exception.getFailureCount()).isEqualTo(2);
        assertThat(exception.getPartialResults()).hasSize(1);
    }

    @Test
    @DisplayName("BatchExecutionException should handle null failed command")
    void batchExecutionExceptionShouldHandleNullFailedCommand() {
        RuntimeException cause = new RuntimeException("error");

        BatchExecutionException exception = new BatchExecutionException(
                "Failure",
                cause,
                null,
                -1,
                Collections.emptyList()
        );

        assertThat(exception.getFailedCommand()).isNull();
        assertThat(exception.getFailedIndex()).isEqualTo(-1);
    }

    @Test
    @DisplayName("BatchExecutionException should reject empty failures list")
    void batchExecutionExceptionShouldRejectEmptyFailures() {
        assertThatThrownBy(() -> new BatchExecutionException(
                "message",
                Collections.emptyList(),
                null,
                -1,
                Collections.emptyList()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("failures must not be empty");
    }

    @Test
    @DisplayName("BatchExecutionException should defensively copy partialResults list")
    void batchExecutionExceptionShouldDefensivelyCopyPartialResults() {
        // Given - a mutable list of partial results
        java.util.ArrayList<String> originalList = new java.util.ArrayList<>();
        originalList.add("result1");
        originalList.add("result2");

        RuntimeException cause = new RuntimeException("test error");

        // When - create exception with the mutable list
        BatchExecutionException exception = new BatchExecutionException(
                "Test failure",
                cause,
                new TestCommand("failed"),
                1,
                originalList
        );

        // Then - modify the original list after exception creation
        originalList.add("result3");
        originalList.remove(0);

        // The exception's internal list should not be affected
        assertThat(exception.getPartialResults())
                .hasSize(2)
                .containsExactly("result1", "result2");
    }

    @Test
    @DisplayName("BatchExecutionException multi-failure constructor should defensively copy partialResults")
    void batchExecutionExceptionMultiFailureShouldDefensivelyCopyPartialResults() {
        // Given - a mutable list of partial results
        java.util.ArrayList<String> originalList = new java.util.ArrayList<>();
        originalList.add("result1");

        List<Throwable> failures = List.of(
                new RuntimeException("error1"),
                new RuntimeException("error2")
        );

        // When - create exception with the mutable list using multi-failure constructor
        BatchExecutionException exception = new BatchExecutionException(
                "Multiple failures",
                failures,
                new TestCommand("failed"),
                1,
                originalList
        );

        // Then - modify the original list after exception creation
        originalList.add("result2");
        originalList.clear();

        // The exception's internal list should not be affected
        assertThat(exception.getPartialResults())
                .hasSize(1)
                .containsExactly("result1");
    }

    @Test
    @DisplayName("BatchExecutionException should reject null message in simple constructor")
    void batchExecutionExceptionShouldRejectNullMessageInSimpleConstructor() {
        assertThatThrownBy(() -> new BatchExecutionException(null, new RuntimeException("cause")))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("message must not be null");
    }

}
