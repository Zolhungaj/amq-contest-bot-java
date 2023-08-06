package tech.zolhungaj.amqcontestbot.exceptions;

public class IncorrectArgumentCountException extends IncorrectCommandUsageException{
    public IncorrectArgumentCountException(int expected) {
        super("chat-command.argument-count.wrong-exact", String.valueOf(expected));
    }

    public IncorrectArgumentCountException(int min, int max) {
        super("chat-command.argument-count.wrong-range", String.valueOf(min), String.valueOf(max));
    }
}
