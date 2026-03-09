package io.iamcyw.tower.quarkus.test;

import io.iamcyw.tower.messaging.Command;
import io.iamcyw.tower.messaging.CommandHandler;
import io.iamcyw.tower.messaging.HandlerMetadata;
import io.iamcyw.tower.messaging.HandlerRegistry;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the Tower Quarkus extension.
 *
 * <p>These tests verify the interface-based CommandHandler pattern with Quarkus:
 * <ul>
 *   <li>Handler discovery at build time via Jandex</li>
 *   <li>Metadata generation via Gizmo</li>
 *   <li>CDI bean registration</li>
 *   <li>Handler registry functionality</li>
 * </ul>
 *
 * @since 2.0
 */
public class TowerQuarkusTest {

    /**
     * Test command for unit testing.
     */
    public static class TestCommand implements Command {
        private final String value;

        public TestCommand(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * Test result for unit testing.
     */
    public static class TestResult {
        private final String processedValue;

        public TestResult(String processedValue) {
            this.processedValue = processedValue;
        }

        public String getProcessedValue() {
            return processedValue;
        }
    }

    /**
     * Test handler implementing the CommandHandler interface.
     */
    @Singleton
    public static class TestCommandHandler implements CommandHandler<TestCommand, TestResult> {
        @Override
        public TestResult handle(TestCommand command) {
            return new TestResult("processed:" + command.getValue());
        }

        @Override
        public boolean canHandle(TestCommand command) {
            return command != null && command.getValue() != null;
        }
    }

    // Start unit test with your extension loaded
    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(
                            Command.class,
                            CommandHandler.class,
                            HandlerMetadata.class,
                            HandlerRegistry.class,
                            TestCommand.class,
                            TestResult.class,
                            TestCommandHandler.class));

    @Inject
    HandlerRegistry handlerRegistry;

    @Inject
    TestCommandHandler testCommandHandler;

    /**
     * Tests that the handler registry is properly initialized.
     */
    @Test
    public void testHandlerRegistryInitialized() {
        assertThat(handlerRegistry).isNotNull();
    }

    /**
     * Tests that the test handler is discovered and injected.
     */
    @Test
    public void testHandlerDiscovered() {
        assertThat(testCommandHandler).isNotNull();
    }

    /**
     * Tests handler lookup via registry.
     */
    @Test
    public void testHandlerLookup() {
        // Given
        TestCommand command = new TestCommand("test-value");

        // When
        var handlers = handlerRegistry.getHandlersForCommand(command);

        // Then
        assertThat(handlers)
                .isNotNull()
                .hasSize(1);
    }

    /**
     * Tests handler execution through registry.
     */
    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void testHandlerExecution() {
        // Given
        TestCommand command = new TestCommand("execution-test");

        // When
        var handlers = handlerRegistry.getHandlersForCommand(command);
        assertThat(handlers).hasSize(1);

        CommandHandler handler = handlers.get(0);
        TestResult result = (TestResult) handler.handle(command);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getProcessedValue()).isEqualTo("processed:execution-test");
    }

    /**
     * Tests that the canHandle filtering works correctly.
     */
    @Test
    public void testCanHandleFiltering() {
        // Given - valid command
        TestCommand validCommand = new TestCommand("valid");

        // When
        var handlers = handlerRegistry.getHandlersForCommand(validCommand);

        // Then
        assertThat(handlers).hasSize(1);

        // Given - invalid command (null value)
        TestCommand invalidCommand = new TestCommand(null);

        // When
        var handlersForInvalid = handlerRegistry.getHandlersForCommand(invalidCommand);

        // Then - handler rejects command via canHandle
        assertThat(handlersForInvalid).isEmpty();
    }

    /**
     * Tests handler type safety.
     */
    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void testHandlerTypeSafety() {
        // Given
        TestCommand command = new TestCommand("type-safety");

        // When
        var handlers = handlerRegistry.getHandlersForCommand(command);
        assertThat(handlers).hasSize(1);

        // Then - verify type-safe access
        CommandHandler handler = handlers.get(0);
        assertThat(handler).isInstanceOf(TestCommandHandler.class);
    }

}
