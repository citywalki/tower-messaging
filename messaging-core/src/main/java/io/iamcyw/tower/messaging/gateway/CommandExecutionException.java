package io.iamcyw.tower.messaging.gateway;

import io.iamcyw.tower.messaging.Command;

/**
 * Exception thrown when a command handler throws an exception during execution.
 *
 * @since 2.0
 */
public class CommandExecutionException extends RuntimeException {

    private final Command command;

    /**
     * Creates a new CommandExecutionException.
     *
     * @param command the command being executed when the exception occurred
     * @param cause the original exception thrown by the handler
     */
    public CommandExecutionException(Command command, Throwable cause) {
        super("Exception executing command " + (command != null ? command.getClass().getName() : "null") + ": " + cause.getMessage(), cause);
        this.command = command;
    }

    /**
     * Returns the command being executed when the exception occurred.
     *
     * @return the command; may be null
     */
    public Command getCommand() {
        return command;
    }

}
