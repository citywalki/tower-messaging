package io.iamcyw.tower.quarkus.it.domain;

import io.iamcyw.tower.messaging.Command;

/**
 * Test command for integration testing.
 *
 * <p>This class implements the {@link Command} interface, making it a valid
 * command that can be processed by {@link io.iamcyw.tower.messaging.CommandHandler}
 * implementations.</p>
 *
 * @see Command
 * @since 2.0
 */
public class BasicTestCommand implements Command {

    private String payload;

    /**
     * Default constructor required for serialization/deserialization.
     */
    public BasicTestCommand() {
    }

    /**
     * Constructs a BasicTestCommand with the given payload.
     *
     * @param payload the payload string to carry in this command
     */
    public BasicTestCommand(String payload) {
        this.payload = payload;
    }

    /**
     * Returns the payload of this command.
     *
     * @return the payload string, may be null
     */
    public String getPayload() {
        return payload;
    }

    /**
     * Sets the payload of this command.
     *
     * @param payload the payload string to set
     */
    public void setPayload(String payload) {
        this.payload = payload;
    }

}
