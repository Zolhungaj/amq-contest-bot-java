package tech.zolhungaj.amqcontestbot.exceptions;

import lombok.Getter;

import java.util.List;

@Getter
public class IncorrectCommandUsageException extends IllegalArgumentException{
    private final String i18nIdentifier;
    private final List<String> arguments;

    public IncorrectCommandUsageException(String i18nIdentifier, Object... arguments) {
        this(i18nIdentifier, List.of(arguments));
    }
    public IncorrectCommandUsageException(String i18nIdentifier, List<Object> arguments) {
        super(i18nIdentifier);
        this.i18nIdentifier = i18nIdentifier;
        this.arguments = arguments.stream().map(String::valueOf).toList();
    }
}
