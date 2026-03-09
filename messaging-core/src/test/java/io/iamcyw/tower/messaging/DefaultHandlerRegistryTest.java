package io.iamcyw.tower.messaging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link DefaultHandlerRegistry}.
 *
 * <p>These tests verify that the registry correctly filters handlers by type
 * and canHandle predicate, supports multiple handlers, and returns empty lists
 * when no handlers match.</p>
 */
class DefaultHandlerRegistryTest {

    /**
     * Test command for registry tests.
     */
    record TestCommand(String value) implements Command {
    }

    /**
     * Another test command type for testing type filtering.
     */
    record OtherCommand(String value) implements Command {
    }

    /**
     * Test result type.
     */
    record TestResult(String value) {
    }

    /**
     * Handler for TestCommand that always accepts.
     */
    static class AlwaysAcceptHandler implements CommandHandler<TestCommand, TestResult> {

        @Override
        public TestResult handle(TestCommand command) {
            return new TestResult("always:" + command.value());
        }

        @Override
        public boolean canHandle(TestCommand command) {
            return true;
        }
    }

    /**
     * Handler for TestCommand that conditionally accepts based on command value.
     */
    static class ConditionalHandler implements CommandHandler<TestCommand, TestResult> {

        @Override
        public TestResult handle(TestCommand command) {
            return new TestResult("conditional:" + command.value());
        }

        @Override
        public boolean canHandle(TestCommand command) {
            return command.value() != null && command.value().startsWith("special");
        }
    }

    /**
     * Handler for OtherCommand.
     */
    static class OtherCommandHandler implements CommandHandler<OtherCommand, TestResult> {

        @Override
        public TestResult handle(OtherCommand command) {
            return new TestResult("other:" + command.value());
        }
    }

    /**
     * Creates metadata for a handler class.
     */
    private static <C extends Command, R> HandlerMetadata<C, R> createMetadata(
            String handlerName,
            Class<C> commandType,
            Class<R> resultType) {
        return createMetadata(handlerName, commandType, resultType, null);
    }

    /**
     * Creates metadata for a handler class with explicit handler class.
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
                if (handlerClass != null) {
                    return handlerClass;
                }
                // Fallback: try to infer from handler name (for tests without explicit class)
                try {
                    Class<?> clazz = Class.forName(handlerName);
                    if (CommandHandler.class.isAssignableFrom(clazz)) {
                        return (Class<? extends CommandHandler<C, R>>) clazz;
                    }
                } catch (ClassNotFoundException e) {
                    // For test classes that might not be found via reflection,
                    // return a placeholder - tests should provide explicit handlerClass
                }
                throw new IllegalStateException(
                        "Cannot determine handler class for: " + handlerName +
                        ". Use createMetadata with explicit handlerClass parameter.");
            }
        };
    }

    @Test
    @DisplayName("Should return empty list when no handlers registered")
    void shouldReturnEmptyListWhenNoHandlersRegistered() {
        // Given - empty registry
        DefaultHandlerRegistry registry = new DefaultHandlerRegistry(
                Collections.emptyList(),
                Collections.emptyList()
        );

        // When - looking up handlers for a command
        TestCommand command = new TestCommand("test");
        List<CommandHandler<TestCommand, TestResult>> handlers =
                registry.getHandlersForCommand(command);

        // Then - should return empty list
        assertThat(handlers).isEmpty();
    }

    @Test
    @DisplayName("Should return handler matching command type")
    void shouldReturnHandlerMatchingCommandType() {
        // Given - a registry with one handler
        AlwaysAcceptHandler handler = new AlwaysAcceptHandler();
        HandlerMetadata<TestCommand, TestResult> metadata = createMetadata(
                handler.getClass().getName(),
                TestCommand.class,
                TestResult.class,
                AlwaysAcceptHandler.class
        );

        DefaultHandlerRegistry registry = new DefaultHandlerRegistry(
                List.of(handler),
                List.of(metadata)
        );

        // When - looking up handlers
        TestCommand command = new TestCommand("test");
        List<CommandHandler<TestCommand, TestResult>> handlers =
                registry.getHandlersForCommand(command);

        // Then - should return the matching handler
        assertThat(handlers).hasSize(1);
        assertThat(handlers.get(0)).isSameAs(handler);
    }

    @Test
    @DisplayName("Should filter handlers by type - exclude handlers for different command types")
    void shouldFilterHandlersByType() {
        // Given - registry with handlers for different command types
        AlwaysAcceptHandler testHandler = new AlwaysAcceptHandler();
        OtherCommandHandler otherHandler = new OtherCommandHandler();

        HandlerMetadata<TestCommand, TestResult> testMetadata = createMetadata(
                testHandler.getClass().getName(),
                TestCommand.class,
                TestResult.class,
                AlwaysAcceptHandler.class
        );
        HandlerMetadata<OtherCommand, TestResult> otherMetadata = createMetadata(
                otherHandler.getClass().getName(),
                OtherCommand.class,
                TestResult.class,
                OtherCommandHandler.class
        );

        DefaultHandlerRegistry registry = new DefaultHandlerRegistry(
                List.of(testHandler, otherHandler),
                List.of(testMetadata, otherMetadata)
        );

        // When - looking up handlers for TestCommand
        TestCommand command = new TestCommand("test");
        List<CommandHandler<TestCommand, TestResult>> handlers =
                registry.getHandlersForCommand(command);

        // Then - should only return TestCommand handler
        assertThat(handlers).hasSize(1);
        assertThat(handlers.get(0)).isSameAs(testHandler);
    }

    @Test
    @DisplayName("Should filter handlers by canHandle predicate")
    void shouldFilterHandlersByCanHandle() {
        // Given - registry with conditional handler
        ConditionalHandler handler = new ConditionalHandler();
        HandlerMetadata<TestCommand, TestResult> metadata = createMetadata(
                handler.getClass().getName(),
                TestCommand.class,
                TestResult.class,
                ConditionalHandler.class
        );

        DefaultHandlerRegistry registry = new DefaultHandlerRegistry(
                List.of(handler),
                List.of(metadata)
        );

        // When - looking up with command that passes canHandle
        TestCommand validCommand = new TestCommand("special-value");
        List<CommandHandler<TestCommand, TestResult>> matchingHandlers =
                registry.getHandlersForCommand(validCommand);

        // When - looking up with command that fails canHandle
        TestCommand invalidCommand = new TestCommand("regular-value");
        List<CommandHandler<TestCommand, TestResult>> nonMatchingHandlers =
                registry.getHandlersForCommand(invalidCommand);

        // Then - should filter based on canHandle
        assertThat(matchingHandlers).hasSize(1);
        assertThat(nonMatchingHandlers).isEmpty();
    }

    @Test
    @DisplayName("Should return multiple handlers when multiple match for conditional routing")
    void shouldReturnMultipleHandlersForConditionalRouting() {
        // Given - registry with multiple handlers for same command type
        AlwaysAcceptHandler handler1 = new AlwaysAcceptHandler();
        AlwaysAcceptHandler handler2 = new AlwaysAcceptHandler();

        HandlerMetadata<TestCommand, TestResult> metadata1 = createMetadata(
                handler1.getClass().getName(),
                TestCommand.class,
                TestResult.class,
                AlwaysAcceptHandler.class
        );
        HandlerMetadata<TestCommand, TestResult> metadata2 = createMetadata(
                handler2.getClass().getName(),
                TestCommand.class,
                TestResult.class,
                AlwaysAcceptHandler.class
        );

        DefaultHandlerRegistry registry = new DefaultHandlerRegistry(
                List.of(handler1, handler2),
                List.of(metadata1, metadata2)
        );

        // When - looking up handlers
        TestCommand command = new TestCommand("test");
        List<CommandHandler<TestCommand, TestResult>> handlers =
                registry.getHandlersForCommand(command);

        // Then - should return both handlers
        assertThat(handlers).hasSize(2);
        assertThat(handlers).containsExactlyInAnyOrder(handler1, handler2);
    }

    @Test
    @DisplayName("Should return empty list when handler has no metadata")
    void shouldReturnEmptyListWhenHandlerHasNoMetadata() {
        // Given - registry with handler but no corresponding metadata
        AlwaysAcceptHandler handler = new AlwaysAcceptHandler();

        DefaultHandlerRegistry registry = new DefaultHandlerRegistry(
                List.of(handler),
                Collections.emptyList()
        );

        // When - looking up handlers
        TestCommand command = new TestCommand("test");
        List<CommandHandler<TestCommand, TestResult>> handlers =
                registry.getHandlersForCommand(command);

        // Then - should return empty list (handler ignored due to missing metadata)
        assertThat(handlers).isEmpty();
    }

    @Test
    @DisplayName("Should throw NullPointerException when command is null")
    void shouldThrowExceptionWhenCommandIsNull() {
        // Given - a registry
        DefaultHandlerRegistry registry = new DefaultHandlerRegistry(
                Collections.emptyList(),
                Collections.emptyList()
        );

        // Then - should throw when command is null
        assertThatThrownBy(() -> registry.getHandlersForCommand(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("command");
    }

    @Test
    @DisplayName("Should throw NullPointerException when handlers list is null")
    void shouldThrowExceptionWhenHandlersListIsNull() {
        // Then - should throw when handlers is null
        assertThatThrownBy(() -> new DefaultHandlerRegistry(
                null,
                Collections.emptyList()
        ))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("handlers");
    }

    @Test
    @DisplayName("Should throw NullPointerException when metadata list is null")
    void shouldThrowExceptionWhenMetadataListIsNull() {
        // Then - should throw when metadata is null
        assertThatThrownBy(() -> new DefaultHandlerRegistry(
                Collections.emptyList(),
                null
        ))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("metadata");
    }

    @Test
    @DisplayName("Should handle duplicate metadata entries gracefully")
    void shouldHandleDuplicateMetadataEntries() {
        // Given - registry with duplicate metadata entries (same handler class)
        AlwaysAcceptHandler handler = new AlwaysAcceptHandler();

        HandlerMetadata<TestCommand, TestResult> metadata1 = createMetadata(
                handler.getClass().getName(),
                TestCommand.class,
                TestResult.class,
                AlwaysAcceptHandler.class
        );
        HandlerMetadata<TestCommand, TestResult> metadata2 = createMetadata(
                handler.getClass().getName() + "_duplicate",
                TestCommand.class,
                TestResult.class,
                AlwaysAcceptHandler.class
        );

        // When - creating registry with duplicates (should not throw)
        DefaultHandlerRegistry registry = new DefaultHandlerRegistry(
                List.of(handler),
                List.of(metadata1, metadata2)
        );

        // Then - lookup should still work
        TestCommand command = new TestCommand("test");
        List<CommandHandler<TestCommand, TestResult>> handlers =
                registry.getHandlersForCommand(command);

        assertThat(handlers).hasSize(1);
        assertThat(handlers.get(0)).isSameAs(handler);
    }

    @Test
    @DisplayName("Should return empty list when all handlers fail canHandle check")
    void shouldReturnEmptyListWhenAllHandlersFailCanHandle() {
        // Given - registry with only conditional handler
        ConditionalHandler handler = new ConditionalHandler();
        HandlerMetadata<TestCommand, TestResult> metadata = createMetadata(
                handler.getClass().getName(),
                TestCommand.class,
                TestResult.class,
                ConditionalHandler.class
        );

        DefaultHandlerRegistry registry = new DefaultHandlerRegistry(
                List.of(handler),
                List.of(metadata)
        );

        // When - looking up with command that fails canHandle
        TestCommand command = new TestCommand("not-matching");
        List<CommandHandler<TestCommand, TestResult>> handlers =
                registry.getHandlersForCommand(command);

        // Then - should return empty list
        assertThat(handlers).isEmpty();
    }

}
