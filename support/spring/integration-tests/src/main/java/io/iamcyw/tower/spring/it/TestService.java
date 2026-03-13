package io.iamcyw.tower.spring.it;

import io.iamcyw.tower.messaging.CommandHandler;
import io.iamcyw.tower.spring.it.domain.BasicTestCommand;
import io.iamcyw.tower.spring.it.domain.BasicTestResult;

/**
 * Test command handler for Spring Boot integration testing.
 *
 * <p>No Spring annotations — discovered automatically by
 * {@link io.iamcyw.tower.spring.CommandHandlerScanRegistrar}.</p>
 *
 * @since 2.0
 */
public class TestService implements CommandHandler<BasicTestCommand, BasicTestResult> {

    @Override
    public BasicTestResult handle(BasicTestCommand command) {
        if (command.getPayload() == null) {
            return new BasicTestResult(null, false);
        }

        String processedPayload = "processed:" + command.getPayload();
        return new BasicTestResult(processedPayload, true);
    }

    @Override
    public boolean canHandle(BasicTestCommand command) {
        return command != null && command.getPayload() != null;
    }

}
