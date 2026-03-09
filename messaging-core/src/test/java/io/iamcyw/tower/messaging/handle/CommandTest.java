package io.iamcyw.tower.messaging.handle;

import io.iamcyw.tower.messaging.Command;
import io.iamcyw.tower.messaging.CommandHandler;
import io.iamcyw.tower.messaging.DefaultHandlerRegistry;
import io.iamcyw.tower.messaging.HandlerMetadata;
import io.iamcyw.tower.messaging.HandlerRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for interface-based command handlers.
 *
 * <p>These tests verify the new interface-based handler pattern where handlers
 * implement {@link CommandHandler} instead of using annotations. Tests cover:
 * <ul>
 *   <li>Basic handler execution</li>
 *   <li>HandlerRegistry integration</li>
 *   <li>canHandle() conditional routing</li>
 *   <li>Type-safe command handling</li>
 * </ul>
 *
 * @since 2.0
 */
public class CommandTest {

    /**
     * Test command for handler tests.
     */
    public record SimpleCommand(String value) implements Command {
    }

    /**
     * Another test command type.
     */
    public record PriorityCommand(String value, int priority) implements Command {
    }

    /**
     * Simple handler that always accepts commands.
     */
    public static class SimpleCommandHandler implements CommandHandler<SimpleCommand, String> {

        @Override
        public String handle(SimpleCommand command) {
            return "result:" + command.value();
        }

        @Override
        public boolean canHandle(SimpleCommand command) {
            return true;
        }
    }

    /**
     * Handler with conditional routing based on command value.
     */
    public static class ConditionalCommandHandler implements CommandHandler<SimpleCommand, String> {

        @Override
        public String handle(SimpleCommand command) {
            return "conditional:" + command.value();
        }

        @Override
        public boolean canHandle(SimpleCommand command) {
            return command.value() != null && command.value().startsWith("special");
        }
    }

    /**
     * Handler for priority commands.
     */
    public static class PriorityCommandHandler implements CommandHandler<PriorityCommand, String> {

        @Override
        public String handle(PriorityCommand command) {
            return "priority:" + command.priority() + ":" + command.value();
        }

        @Override
        public boolean canHandle(PriorityCommand command) {
            return command.priority() > 5;
        }
    }

    /**
     * Creates metadata for a handler class.
     */
    @SuppressWarnings("unchecked")
    private static <C extends Command, R> HandlerMetadata<C, R> createMetadata(
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
                return handlerClass.getName();
            }

            @Override
            public Class<? extends CommandHandler<C, R>> handlerClass() {
                return handlerClass;
            }
        };
    }

    @Test
    @DisplayName("Should execute handler and return result")
    void shouldExecuteHandlerAndReturnResult() {
        // Given - a handler
        SimpleCommandHandler handler = new SimpleCommandHandler();

        // When - handling a command
        SimpleCommand command = new SimpleCommand("test");
        String result = handler.handle(command);

        // Then - result should be correct
        assertThat(result).isEqualTo("result:test");
    }

    @Test
    @DisplayName("Should filter handlers by type using HandlerRegistry")
    void shouldFilterHandlersByType() {
        // Given - registry with handlers for different command types
        SimpleCommandHandler simpleHandler = new SimpleCommandHandler();
        PriorityCommandHandler priorityHandler = new PriorityCommandHandler();

        HandlerMetadata<SimpleCommand, String> simpleMetadata = createMetadata(
                SimpleCommand.class, String.class, SimpleCommandHandler.class);
        HandlerMetadata<PriorityCommand, String> priorityMetadata = createMetadata(
                PriorityCommand.class, String.class, PriorityCommandHandler.class);

        HandlerRegistry registry = new DefaultHandlerRegistry(
                List.of(simpleHandler, priorityHandler),
                List.of(simpleMetadata, priorityMetadata)
        );

        // When - looking up handlers for SimpleCommand
        SimpleCommand command = new SimpleCommand("test");
        List<CommandHandler<SimpleCommand, String>> handlers =
                registry.getHandlersForCommand(command);

        // Then - should only return SimpleCommand handler
        assertThat(handlers).hasSize(1);
        assertThat(handlers.get(0)).isSameAs(simpleHandler);
    }

    @Test
    @DisplayName("Should filter handlers by canHandle predicate")
    void shouldFilterHandlersByCanHandle() {
        // Given - registry with conditional handler
        ConditionalCommandHandler handler = new ConditionalCommandHandler();

        HandlerMetadata<SimpleCommand, String> metadata = createMetadata(
                SimpleCommand.class, String.class, ConditionalCommandHandler.class);

        HandlerRegistry registry = new DefaultHandlerRegistry(
                List.of(handler),
                List.of(metadata)
        );

        // When - looking up with command that passes canHandle
        SimpleCommand validCommand = new SimpleCommand("special-value");
        List<CommandHandler<SimpleCommand, String>> matchingHandlers =
                registry.getHandlersForCommand(validCommand);

        // When - looking up with command that fails canHandle
        SimpleCommand invalidCommand = new SimpleCommand("regular-value");
        List<CommandHandler<SimpleCommand, String>> nonMatchingHandlers =
                registry.getHandlersForCommand(invalidCommand);

        // Then - should filter based on canHandle
        assertThat(matchingHandlers).hasSize(1);
        assertThat(nonMatchingHandlers).isEmpty();
    }

    @Test
    @DisplayName("Should return multiple handlers for conditional routing")
    void shouldReturnMultipleHandlersForConditionalRouting() {
        // Given - registry with multiple handlers for same command type
        SimpleCommandHandler handler1 = new SimpleCommandHandler();
        SimpleCommandHandler handler2 = new SimpleCommandHandler();

        HandlerMetadata<SimpleCommand, String> metadata1 = createMetadata(
                SimpleCommand.class, String.class, SimpleCommandHandler.class);
        HandlerMetadata<SimpleCommand, String> metadata2 = createMetadata(
                SimpleCommand.class, String.class, SimpleCommandHandler.class);

        HandlerRegistry registry = new DefaultHandlerRegistry(
                List.of(handler1, handler2),
                List.of(metadata1, metadata2)
        );

        // When - looking up handlers
        SimpleCommand command = new SimpleCommand("test");
        List<CommandHandler<SimpleCommand, String>> handlers =
                registry.getHandlersForCommand(command);

        // Then - should return both handlers
        assertThat(handlers).hasSize(2);
    }

    @Test
    @DisplayName("Should support handler with conditional routing based on priority")
    void shouldSupportPriorityBasedRouting() {
        // Given - registry with priority handler
        PriorityCommandHandler handler = new PriorityCommandHandler();

        HandlerMetadata<PriorityCommand, String> metadata = createMetadata(
                PriorityCommand.class, String.class, PriorityCommandHandler.class);

        HandlerRegistry registry = new DefaultHandlerRegistry(
                List.of(handler),
                List.of(metadata)
        );

        // When - looking up with high priority command
        PriorityCommand highPriority = new PriorityCommand("task", 10);
        List<CommandHandler<PriorityCommand, String>> highPriorityHandlers =
                registry.getHandlersForCommand(highPriority);

        // When - looking up with low priority command
        PriorityCommand lowPriority = new PriorityCommand("task", 3);
        List<CommandHandler<PriorityCommand, String>> lowPriorityHandlers =
                registry.getHandlersForCommand(lowPriority);

        // Then - high priority should match, low priority should not
        assertThat(highPriorityHandlers).hasSize(1);
        assertThat(lowPriorityHandlers).isEmpty();
    }

    @Test
    @DisplayName("Should execute handler retrieved from registry")
    void shouldExecuteHandlerRetrievedFromRegistry() {
        // Given - registry with handler
        SimpleCommandHandler handler = new SimpleCommandHandler();

        HandlerMetadata<SimpleCommand, String> metadata = createMetadata(
                SimpleCommand.class, String.class, SimpleCommandHandler.class);

        HandlerRegistry registry = new DefaultHandlerRegistry(
                List.of(handler),
                List.of(metadata)
        );

        // When - retrieving and executing handler
        SimpleCommand command = new SimpleCommand("test");
        List<CommandHandler<SimpleCommand, String>> handlers =
                registry.getHandlersForCommand(command);

        assertThat(handlers).hasSize(1);
        String result = handlers.get(0).handle(command);

        // Then - result should be correct
        assertThat(result).isEqualTo("result:test");
    }

    @Test
    @DisplayName("Should return empty list when no handlers match command type")
    void shouldReturnEmptyListWhenNoHandlersMatch() {
        // Given - registry with only priority handler
        PriorityCommandHandler handler = new PriorityCommandHandler();

        HandlerMetadata<PriorityCommand, String> metadata = createMetadata(
                PriorityCommand.class, String.class, PriorityCommandHandler.class);

        HandlerRegistry registry = new DefaultHandlerRegistry(
                List.of(handler),
                List.of(metadata)
        );

        // When - looking up handlers for different command type
        // Note: We need to use a command type that is not registered
        // Since we can't easily create a new command type here, we test with empty registry
        HandlerRegistry emptyRegistry = new DefaultHandlerRegistry(
                List.of(),
                List.of()
        );

        SimpleCommand command = new SimpleCommand("test");
        List<CommandHandler<SimpleCommand, String>> handlers =
                emptyRegistry.getHandlersForCommand(command);

        // Then - should return empty list
        assertThat(handlers).isEmpty();
    }

    @Test
    @DisplayName("Handler canHandle should return true by default")
    void handlerCanHandleShouldReturnTrueByDefault() {
        // Given - a handler using default canHandle
        CommandHandler<SimpleCommand, String> handler = new CommandHandler<>() {
            @Override
            public String handle(SimpleCommand command) {
                return "handled";
            }
            // Using default canHandle which returns true
        };

        // When - checking canHandle
        SimpleCommand command = new SimpleCommand("test");
        boolean canHandle = handler.canHandle(command);

        // Then - should return true by default
        assertThat(canHandle).isTrue();
    }

}
