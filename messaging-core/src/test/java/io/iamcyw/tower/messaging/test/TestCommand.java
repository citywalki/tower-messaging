package io.iamcyw.tower.messaging.test;

import io.iamcyw.tower.messaging.Command;

/**
 * Test command implementing the Command interface for interface-based handler testing.
 *
 * <p>This class demonstrates the new pattern where commands explicitly implement
 * the {@link Command} marker interface for type safety.</p>
 *
 * @since 2.0
 */
public class TestCommand implements Command {

    private String payload;

    /**
     * Default constructor for serialization frameworks.
     */
    public TestCommand() {
    }

    /**
     * Constructs a TestCommand with the given payload.
     *
     * @param payload the command payload
     */
    public TestCommand(String payload) {
        this.payload = payload;
    }

    /**
     * Returns the command payload.
     *
     * @return the payload string
     */
    public String getPayload() {
        return payload;
    }

    /**
     * Sets the command payload.
     *
     * @param payload the payload string
     */
    public void setPayload(String payload) {
        this.payload = payload;
    }

}
