package io.iamcyw.tower.spring.it.domain;

import io.iamcyw.tower.messaging.Command;

/**
 * Test command for Spring Boot integration testing.
 *
 * @since 2.0
 */
public class BasicTestCommand implements Command {

    private String payload;

    public BasicTestCommand() {
    }

    public BasicTestCommand(String payload) {
        this.payload = payload;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

}
