package io.iamcyw.tower.quarkus.it;

import io.iamcyw.tower.messaging.gateway.MessageGateway;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;

@QuarkusTest
class BasicTest {

    @Inject
    MessageGateway commandGateway;

    @Test
    void testBasicVoidCommand() {
        // TODO: Add command test
        // commandGateway.send(new BasicTestCommand("payload"));
    }

}
