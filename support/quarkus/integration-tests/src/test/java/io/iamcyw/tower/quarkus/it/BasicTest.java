package io.iamcyw.tower.quarkus.it;

import io.iamcyw.tower.messaging.Command;
import io.iamcyw.tower.messaging.CommandHandler;
import io.iamcyw.tower.messaging.HandlerMetadata;
import io.iamcyw.tower.messaging.HandlerRegistry;
import io.iamcyw.tower.quarkus.it.domain.BasicTestCommand;
import io.iamcyw.tower.quarkus.it.domain.BasicTestResult;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Tower Messaging Quarkus extension.
 *
 * <p>These tests verify the interface-based CommandHandler pattern with Quarkus:
 * <ul>
 *   <li>Handler discovery via CDI</li>
 *   <li>Handler registry functionality</li>
 *   <li>Command dispatch and handling</li>
 *   <li>End-to-end command processing</li>
 * </ul>
 *
 * @since 2.0
 */
@QuarkusTest
class BasicTest {

    @Inject
    HandlerRegistry handlerRegistry;

    @Inject
    TestService testService;

    /**
     * Tests that the handler registry is properly initialized and can find handlers.
     */
    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void testHandlerRegistryDiscovery() {
        // Given
        BasicTestCommand command = new BasicTestCommand("test-payload");

        // When
        var handlers = handlerRegistry.getHandlersForCommand(command);

        // Then
        assertThat(handlers)
                .isNotNull()
                .hasSize(1);

        CommandHandler handler = handlers.get(0);
        assertThat(handler).isNotNull();
    }

    /**
     * Tests that the TestService handler is discovered and registered.
     */
    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void testHandlerIsRegistered() {
        // Given
        BasicTestCommand command = new BasicTestCommand("test-payload");

        // When
        List handlers = handlerRegistry.getHandlersForCommand(command);

        // Then
        assertThat(handlers)
                .isNotEmpty();

        // Check that at least one handler is an instance of TestService
        boolean foundTestService = handlers.stream()
                .anyMatch(h -> h instanceof TestService);
        assertThat(foundTestService).isTrue();
    }

    /**
     * Tests direct handler invocation with type safety.
     */
    @Test
    void testDirectHandlerInvocation() {
        // Given
        BasicTestCommand command = new BasicTestCommand("direct-test");

        // When
        BasicTestResult result = testService.handle(command);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getProcessedPayload()).isEqualTo("processed:direct-test");
    }

    /**
     * Tests handler invocation through the registry.
     */
    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void testHandlerInvocationThroughRegistry() {
        // Given
        BasicTestCommand command = new BasicTestCommand("registry-test");

        // When
        var handlers = handlerRegistry.getHandlersForCommand(command);
        assertThat(handlers).isNotEmpty();

        CommandHandler handler = handlers.get(0);
        BasicTestResult result = (BasicTestResult) handler.handle(command);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getProcessedPayload()).isEqualTo("processed:registry-test");
    }

    /**
     * Tests the canHandle conditional routing logic.
     */
    @Test
    void testCanHandleFiltering() {
        // Given - a command with null payload should be rejected
        BasicTestCommand nullPayloadCommand = new BasicTestCommand(null);

        // When
        boolean canHandle = testService.canHandle(nullPayloadCommand);

        // Then
        assertThat(canHandle).isFalse();

        // Given - a command with non-null payload should be accepted
        BasicTestCommand validCommand = new BasicTestCommand("valid");

        // When
        boolean canHandleValid = testService.canHandle(validCommand);

        // Then
        assertThat(canHandleValid).isTrue();
    }

    /**
     * Tests that the handler registry returns empty list for unknown command types.
     */
    @Test
    void testUnknownCommandReturnsEmptyHandlers() {
        // Given - a command type not handled by any handler
        Command unknownCommand = new Command() {};

        // When
        var handlers = handlerRegistry.getHandlersForCommand(unknownCommand);

        // Then
        assertThat(handlers).isEmpty();
    }

    /**
     * Tests end-to-end command processing flow.
     */
    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void testEndToEndCommandProcessing() {
        // Given
        String payload = "end-to-end-test";
        BasicTestCommand command = new BasicTestCommand(payload);

        // When - get handler from registry
        var handlers = handlerRegistry.getHandlersForCommand(command);
        assertThat(handlers).hasSize(1);

        // And - execute the handler
        CommandHandler handler = handlers.get(0);
        BasicTestResult result = (BasicTestResult) handler.handle(command);

        // Then - verify the complete flow
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getProcessedPayload()).isEqualTo("processed:" + payload);
    }

}
