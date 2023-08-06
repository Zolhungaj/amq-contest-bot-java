package tech.zolhungaj.amqcontestbot.exceptions;

public class IncorrectArgumentCountException extends IncorrectCommandUsageException{
    public IncorrectArgumentCountException(int expected) {
        super("chat-command.argument-count.wrong", String.valueOf(expected));
    }
}
