package io.iamcyw.tower.quarkus.deployment.resolver;

/**
 * Exception thrown when handler type resolution fails.
 *
 * @since 2.0
 */
public class HandlerTypeResolutionException extends RuntimeException {

    public HandlerTypeResolutionException(String message) {
        super(message);
    }

    public HandlerTypeResolutionException(String message, Throwable cause) {
        super(message, cause);
    }

}
