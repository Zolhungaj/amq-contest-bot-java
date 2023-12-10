package tech.zolhungaj.amqcontestbot.exceptions;

import java.util.stream.Stream;

public class IncorrectArgumentCountException extends IncorrectCommandUsageException{
    public IncorrectArgumentCountException() {
        super("chat-command.argument-count.no-arguments");
    }
    public IncorrectArgumentCountException(int expected) {
        super("chat-command.argument-count.wrong-exact", String.valueOf(expected));
    }

    public IncorrectArgumentCountException(int... expected) {
        super("chat-command.argument-count.wrong-exact-list", String.join(",", Stream.of(expected).map(String::valueOf).toList()));
    }

    public IncorrectArgumentCountException(int min, int max) {
        super("chat-command.argument-count.wrong-range", String.valueOf(min), String.valueOf(max));
    }
}
