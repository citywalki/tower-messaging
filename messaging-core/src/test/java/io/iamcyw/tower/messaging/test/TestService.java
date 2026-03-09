package io.iamcyw.tower.messaging.test;

import io.iamcyw.tower.messaging.CommandHandler;

/**
 * Test service implementing CommandHandler interface for interface-based testing.
 *
 * <p>This class demonstrates the new pattern where handlers explicitly implement
 * the {@link CommandHandler} interface instead of using the deprecated @UseCase
 * annotation. This provides compile-time type safety and explicit contracts.</p>
 *
 * <p>The handler processes TestCommand and returns a String result.</p>
 *
 * @since 2.0
 */
public class TestService implements CommandHandler<TestCommand, String> {

    /**
     * Handles the test command by returning a processed result.
     *
     * @param command the command to process; never null
     * @return the processed result string
     */
    @Override
    public String handle(TestCommand command) {
        if (command.getPayload() == null) {
            return "null";
        }
        return "handled:" + command.getPayload();
    }

    /**
     * Determines if this handler can process the given command.
     *
     * <p>This implementation accepts all non-null commands with non-null payload.</p>
     *
     * @param command the command to evaluate; never null
     * @return true if the command has a non-null payload
     */
    @Override
    public boolean canHandle(TestCommand command) {
        return command != null && command.getPayload() != null;
    }

}
