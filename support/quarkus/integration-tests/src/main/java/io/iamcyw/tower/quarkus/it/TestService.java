package io.iamcyw.tower.quarkus.it;

import io.iamcyw.tower.messaging.CommandHandler;
import io.iamcyw.tower.quarkus.it.domain.BasicTestCommand;
import io.iamcyw.tower.quarkus.it.domain.BasicTestResult;
import jakarta.inject.Singleton;
import org.jboss.logging.Logger;

/**
 * Test command handler implementing the interface-based pattern.
 *
 * <p>This class demonstrates the new {@link CommandHandler} interface pattern
 * for handling commands in the Tower Messaging framework. It replaces the
 * old annotation-driven approach with an explicit interface implementation.</p>
 *
 * @see CommandHandler
 * @see BasicTestCommand
 * @see BasicTestResult
 * @since 2.0
 */
@Singleton
public class TestService implements CommandHandler<BasicTestCommand, BasicTestResult> {

    private static final Logger LOGGER = Logger.getLogger(TestService.class);

    /**
     * Handles the BasicTestCommand and returns a BasicTestResult.
     *
     * <p>This method processes the command by logging the payload and returning
     * a result indicating success. It demonstrates the core command handling
     * pattern with compile-time type safety.</p>
     *
     * @param command the command to process; never null
     * @return the result of processing the command
     */
    @Override
    public BasicTestResult handle(BasicTestCommand command) {
        LOGGER.infof("Processing command: %s with payload: %s",
                     command.getClass().getSimpleName(),
                     command.getPayload());

        // Validate the command
        if (command.getPayload() == null) {
            LOGGER.warn("Command payload is null");
            return new BasicTestResult(null, false);
        }

        // Process the command
        String processedPayload = "processed:" + command.getPayload();
        LOGGER.infof("Command processed successfully: %s", processedPayload);

        return new BasicTestResult(processedPayload, true);
    }

    /**
     * Determines whether this handler can process the given command.
     *
     * <p>This implementation accepts all BasicTestCommand instances with non-null
     * payloads. This demonstrates the conditional routing capability of the
     * CommandHandler interface.</p>
     *
     * @param command the command to evaluate; never null
     * @return true if the command has a non-null payload, false otherwise
     */
    @Override
    public boolean canHandle(BasicTestCommand command) {
        return command != null && command.getPayload() != null;
    }

}
