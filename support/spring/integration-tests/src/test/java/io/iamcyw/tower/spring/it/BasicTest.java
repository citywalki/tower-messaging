package io.iamcyw.tower.spring.it;

import io.iamcyw.tower.messaging.Command;
import io.iamcyw.tower.messaging.CommandHandler;
import io.iamcyw.tower.messaging.HandlerRegistry;
import io.iamcyw.tower.messaging.gateway.MessageGateway;
import io.iamcyw.tower.spring.it.domain.BasicTestCommand;
import io.iamcyw.tower.spring.it.domain.BasicTestResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Tower Messaging Spring Boot starter.
 *
 * @since 2.0
 */
@SpringBootTest(classes = TestApplication.class)
class BasicTest {

    @Autowired
    HandlerRegistry handlerRegistry;

    @Autowired
    MessageGateway messageGateway;

    @Autowired
    TestService testService;

    @Test
    void testHandlerRegistryDiscovery() {
        BasicTestCommand command = new BasicTestCommand("test-payload");

        var handlers = handlerRegistry.getHandlersForCommand(command);

        assertThat(handlers)
                .isNotNull()
                .hasSize(1);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void testHandlerIsRegistered() {
        BasicTestCommand command = new BasicTestCommand("test-payload");

        List handlers = handlerRegistry.getHandlersForCommand(command);

        assertThat(handlers).isNotEmpty();

        boolean foundTestService = handlers.stream()
                .anyMatch(h -> h instanceof TestService);
        assertThat(foundTestService).isTrue();
    }

    @Test
    void testDirectHandlerInvocation() {
        BasicTestCommand command = new BasicTestCommand("direct-test");

        BasicTestResult result = testService.handle(command);

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getProcessedPayload()).isEqualTo("processed:direct-test");
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void testHandlerInvocationThroughRegistry() {
        BasicTestCommand command = new BasicTestCommand("registry-test");

        var handlers = handlerRegistry.getHandlersForCommand(command);
        assertThat(handlers).isNotEmpty();

        CommandHandler handler = handlers.get(0);
        BasicTestResult result = (BasicTestResult) handler.handle(command);

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getProcessedPayload()).isEqualTo("processed:registry-test");
    }

    @Test
    void testCanHandleFiltering() {
        BasicTestCommand nullPayloadCommand = new BasicTestCommand(null);
        assertThat(testService.canHandle(nullPayloadCommand)).isFalse();

        BasicTestCommand validCommand = new BasicTestCommand("valid");
        assertThat(testService.canHandle(validCommand)).isTrue();
    }

    @Test
    void testUnknownCommandReturnsEmptyHandlers() {
        Command unknownCommand = new Command() {};

        var handlers = handlerRegistry.getHandlersForCommand(unknownCommand);

        assertThat(handlers).isEmpty();
    }

    @Test
    void testMessageGatewaySend() {
        BasicTestCommand command = new BasicTestCommand("gateway-test");

        // send() should not throw — handler exists and can handle this command
        messageGateway.send(command);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void testEndToEndCommandProcessing() {
        String payload = "end-to-end-test";
        BasicTestCommand command = new BasicTestCommand(payload);

        var handlers = handlerRegistry.getHandlersForCommand(command);
        assertThat(handlers).hasSize(1);

        CommandHandler handler = handlers.get(0);
        BasicTestResult result = (BasicTestResult) handler.handle(command);

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getProcessedPayload()).isEqualTo("processed:" + payload);
    }

}
