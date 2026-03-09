package io.iamcyw.tower.messaging.cdi;

import io.iamcyw.tower.messaging.Command;
import io.iamcyw.tower.messaging.CommandHandler;
import io.iamcyw.tower.messaging.HandlerMetadata;
import io.iamcyw.tower.messaging.HandlerRegistry;
import io.iamcyw.tower.messaging.cdi.producer.HandlerRegistryProducer;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CDI integration tests for {@link HandlerRegistry}.
 *
 * <p>These tests verify that the {@link HandlerRegistryProducer} correctly:</p>
 * <ul>
 *   <li>Collects all CDI-managed {@link CommandHandler} implementations</li>
 *   <li>Collects all CDI-managed {@link HandlerMetadata} instances</li>
 *   <li>Creates a properly configured {@link HandlerRegistry}</li>
 *   <li>Enables type-safe handler lookup through CDI injection</li>
 * </ul>
 */
class HandlerRegistryCdiTest {

    /**
     * Test command type for CDI integration tests.
     */
    public record TestCommand(String value) implements Command {
    }

    /**
     * Another test command type for testing multiple handlers.
     */
    public record OtherCommand(String value) implements Command {
    }

    /**
     * Test result type.
     */
    public record TestResult(String value) {
    }

    /**
     * CDI-managed command handler for TestCommand.
     */
    @Singleton
    public static class TestCommandHandler implements CommandHandler<TestCommand, TestResult> {

        @Override
        public TestResult handle(TestCommand command) {
            return new TestResult("handled:" + command.value());
        }

        @Override
        public boolean canHandle(TestCommand command) {
            return command.value() != null && !command.value().isEmpty();
        }
    }

    /**
     * CDI-managed command handler for OtherCommand.
     */
    @Singleton
    public static class OtherCommandHandler implements CommandHandler<OtherCommand, TestResult> {

        @Override
        public TestResult handle(OtherCommand command) {
            return new TestResult("other:" + command.value());
        }
    }

    /**
     * Metadata producer for TestCommandHandler.
     */
    @Singleton
    public static class TestHandlerMetadataProducer {

        @Produces
        @Singleton
        public HandlerMetadata<TestCommand, TestResult> produceTestHandlerMetadata() {
            return new HandlerMetadata<>() {
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
                    return TestCommandHandler.class.getName();
                }

                @Override
                @SuppressWarnings("unchecked")
                public Class<? extends CommandHandler<TestCommand, TestResult>> handlerClass() {
                    return TestCommandHandler.class;
                }
            };
        }
    }

    /**
     * Metadata producer for OtherCommandHandler.
     */
    @Singleton
    public static class OtherHandlerMetadataProducer {

        @Produces
        @Singleton
        public HandlerMetadata<OtherCommand, TestResult> produceOtherHandlerMetadata() {
            return new HandlerMetadata<>() {
                @Override
                public Class<OtherCommand> commandType() {
                    return OtherCommand.class;
                }

                @Override
                public Class<TestResult> resultType() {
                    return TestResult.class;
                }

                @Override
                public String handlerName() {
                    return OtherCommandHandler.class.getName();
                }

                @Override
                @SuppressWarnings("unchecked")
                public Class<? extends CommandHandler<OtherCommand, TestResult>> handlerClass() {
                    return OtherCommandHandler.class;
                }
            };
        }
    }

    private WeldContainer container;
    private HandlerRegistry handlerRegistry;

    @BeforeEach
    void setUp() {
        Weld weld = new Weld()
                .addBeanClass(HandlerRegistryProducer.class)
                .addBeanClass(TestCommandHandler.class)
                .addBeanClass(OtherCommandHandler.class)
                .addBeanClass(TestHandlerMetadataProducer.class)
                .addBeanClass(OtherHandlerMetadataProducer.class);

        container = weld.initialize();
        handlerRegistry = container.select(HandlerRegistry.class).get();
    }

    @AfterEach
    void tearDown() {
        if (container != null && container.isRunning()) {
            container.shutdown();
        }
    }

    @Test
    @DisplayName("Should inject HandlerRegistry as CDI bean")
    void shouldInjectHandlerRegistryAsCdiBean() {
        // Then - registry should be injected and functional
        assertThat(handlerRegistry).isNotNull();
    }

    @Test
    @DisplayName("Should collect all CDI-managed handlers")
    void shouldCollectAllCdiManagedHandlers() {
        // Given - a command that matches TestCommandHandler
        TestCommand command = new TestCommand("test-value");

        // When - looking up handlers
        List<CommandHandler<TestCommand, TestResult>> handlers =
                handlerRegistry.getHandlersForCommand(command);

        // Then - should find the TestCommandHandler
        assertThat(handlers).hasSize(1);
        assertThat(handlers.get(0)).isInstanceOf(TestCommandHandler.class);
    }

    @Test
    @DisplayName("Should filter handlers by command type")
    void shouldFilterHandlersByCommandType() {
        // Given - commands for different types
        TestCommand testCommand = new TestCommand("test");
        OtherCommand otherCommand = new OtherCommand("other");

        // When - looking up handlers
        List<CommandHandler<TestCommand, TestResult>> testHandlers =
                handlerRegistry.getHandlersForCommand(testCommand);
        List<CommandHandler<OtherCommand, TestResult>> otherHandlers =
                handlerRegistry.getHandlersForCommand(otherCommand);

        // Then - should return only matching handlers
        assertThat(testHandlers).hasSize(1);
        assertThat(testHandlers.get(0)).isInstanceOf(TestCommandHandler.class);

        assertThat(otherHandlers).hasSize(1);
        assertThat(otherHandlers.get(0)).isInstanceOf(OtherCommandHandler.class);
    }

    @Test
    @DisplayName("Should apply canHandle predicate from handlers")
    void shouldApplyCanHandlePredicate() {
        // Given - commands that pass and fail canHandle check
        TestCommand validCommand = new TestCommand("valid-value");
        TestCommand invalidCommand = new TestCommand(""); // Empty string fails canHandle

        // When - looking up handlers
        List<CommandHandler<TestCommand, TestResult>> validHandlers =
                handlerRegistry.getHandlersForCommand(validCommand);
        List<CommandHandler<TestCommand, TestResult>> invalidHandlers =
                handlerRegistry.getHandlersForCommand(invalidCommand);

        // Then - should filter based on canHandle
        assertThat(validHandlers).hasSize(1);
        assertThat(invalidHandlers).isEmpty();
    }

    @Test
    @DisplayName("Should return empty list when no handlers match")
    void shouldReturnEmptyListWhenNoHandlersMatch() {
        // Given - a command with no registered handler (using an anonymous command type)
        Command unhandledCommand = new Command() {
            @Override
            public String toString() {
                return "unhandled";
            }
        };

        // When - looking up handlers
        @SuppressWarnings("unchecked")
        List<CommandHandler<Command, Object>> handlers =
                handlerRegistry.getHandlersForCommand(unhandledCommand);

        // Then - should return empty list
        assertThat(handlers).isEmpty();
    }

    @Test
    @DisplayName("Should use metadata for type matching")
    void shouldUseMetadataForTypeMatching() {
        // Given - a valid command
        TestCommand command = new TestCommand("test");

        // When - looking up handlers
        List<CommandHandler<TestCommand, TestResult>> handlers =
                handlerRegistry.getHandlersForCommand(command);

        // Then - handler should be found (metadata links handler to command type)
        assertThat(handlers).hasSize(1);

        // And - the handler should be functional
        TestResult result = handlers.get(0).handle(command);
        assertThat(result.value()).isEqualTo("handled:test");
    }

}
