package io.iamcyw.tower.messaging.gateway;

import io.iamcyw.tower.messaging.Command;

/**
 * Exception thrown when no handler is found for a command.
 *
 * @since 2.0
 */
public class NoHandlerFoundException extends RuntimeException {

    private final Command command;

    /**
     * Creates a new NoHandlerFoundException.
     *
     * @param command the command that could not be handled
     */
    public NoHandlerFoundException(Command command) {
        super("No handler found for command: " + (command != null ? command.getClass().getName() : "null"));
        this.command = command;
    }

    /**
     * Returns the command that could not be handled.
     *
     * @return the command; may be null
     */
    public Command getCommand() {
        return command;
    }

}
