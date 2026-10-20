package dev.ricodevvv.riven.command;

/**
 * Throw it from a command to stop and show the message to the sender. No stack trace, no fuss.
 */
public class CommandException extends RuntimeException {

    public CommandException(String message) {
        super(message, null, false, false);
    }

}
