package io.iamcyw.tower.messaging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the core command handler interfaces.
 *
 * <p>These tests verify that the interfaces are correctly defined with proper
 * generic constraints and method signatures.</p>
 */
class CommandHandlerInterfaceTest {

    /**
     * Test command for verifying interface contracts.
     */
    record TestCommand(String value) implements Command {
    }

    /**
     * Test result type for verifying interface contracts.
     */
    record TestResult(String value) {
    }

    /**
     * Concrete handler implementation for testing.
     */
    static class TestCommandHandler implements CommandHandler<TestCommand, TestResult> {

        @Override
        public TestResult handle(TestCommand command) {
            return new TestResult(command.value());
        }

        @Override
        public boolean canHandle(TestCommand command) {
            return command.value() != null && !command.value().isEmpty();
        }
    }

    /**
     * Handler that uses the default canHandle implementation.
     */
    static class DefaultCanHandleHandler implements CommandHandler<TestCommand, TestResult> {

        @Override
        public TestResult handle(TestCommand command) {
            return new TestResult(command.value());
        }
    }

    @Test
    @DisplayName("Command interface should be a marker interface")
    void commandShouldBeMarkerInterface() {
        // Given - a command implementation
        TestCommand command = new TestCommand("test");

        // Then - it should implement Command
        assertThat(command).isInstanceOf(Command.class);
    }

    @Test
    @DisplayName("CommandHandler should enforce type-safe handle method")
    void commandHandlerShouldHaveTypeSafeHandleMethod() {
        // Given - a concrete handler
        TestCommandHandler handler = new TestCommandHandler();
        TestCommand command = new TestCommand("test-value");

        // When - handling a command
        TestResult result = handler.handle(command);

        // Then - the result should be type-safe
        assertThat(result).isNotNull();
        assertThat(result.value()).isEqualTo("test-value");
    }

    @Test
    @DisplayName("CommandHandler default canHandle should return true")
    void commandHandlerDefaultCanHandleShouldReturnTrue() {
        // Given - a handler using default canHandle
        DefaultCanHandleHandler handler = new DefaultCanHandleHandler();
        TestCommand command = new TestCommand("any-value");

        // When - checking canHandle
        boolean canHandle = handler.canHandle(command);

        // Then - should return true by default
        assertThat(canHandle).isTrue();
    }

    @Test
    @DisplayName("CommandHandler canHandle can be overridden for conditional routing")
    void commandHandlerCanHandleCanBeOverridden() {
        // Given - a handler with custom canHandle logic
        TestCommandHandler handler = new TestCommandHandler();

        // When - checking with valid command
        TestCommand validCommand = new TestCommand("valid");
        boolean canHandleValid = handler.canHandle(validCommand);

        // When - checking with invalid command (empty string)
        TestCommand invalidCommand = new TestCommand("");
        boolean canHandleInvalid = handler.canHandle(invalidCommand);

        // Then - custom logic should be applied
        assertThat(canHandleValid).isTrue();
        assertThat(canHandleInvalid).isFalse();
    }

    @Test
    @DisplayName("HandlerMetadata should provide type information")
    void handlerMetadataShouldProvideTypeInformation() {
        // Given - a metadata implementation
        HandlerMetadata<TestCommand, TestResult> metadata = new HandlerMetadata<>() {
            @Override
            public Class<TestCommand> commandType() {
                return TestCommand.class;
            }

            @Override
            public Class<TestResult> resultType() {
                return TestResult.class;
            }

            @Override
            public String handlerName() {
                return "TestHandler";
            }

            @Override
            @SuppressWarnings("unchecked")
            public Class<? extends CommandHandler<TestCommand, TestResult>> handlerClass() {
                return (Class<? extends CommandHandler<TestCommand, TestResult>>)
                        TestCommandHandler.class;
            }
        };

        // Then - type information should be accessible
        assertThat(metadata.commandType()).isEqualTo(TestCommand.class);
        assertThat(metadata.resultType()).isEqualTo(TestResult.class);
        assertThat(metadata.handlerName()).isEqualTo("TestHandler");
        assertThat(metadata.handlerClass()).isEqualTo(TestCommandHandler.class);
    }

    @Test
    @DisplayName("HandlerMetadata commandType should support isInstance check")
    void handlerMetadataCommandTypeShouldSupportIsInstance() {
        // Given - metadata and a command instance
        HandlerMetadata<TestCommand, TestResult> metadata = new HandlerMetadata<>() {
            @Override
            public Class<TestCommand> commandType() {
                return TestCommand.class;
            }

            @Override
            public Class<TestResult> resultType() {
                return TestResult.class;
            }

            @Override
            public String handlerName() {
                return "TestHandler";
            }

            @Override
            @SuppressWarnings("unchecked")
            public Class<? extends CommandHandler<TestCommand, TestResult>> handlerClass() {
                return (Class<? extends CommandHandler<TestCommand, TestResult>>)
                        TestCommandHandler.class;
            }
        };

        TestCommand command = new TestCommand("test");

        // Then - isInstance should work correctly
        assertThat(metadata.commandType().isInstance(command)).isTrue();
    }

}
