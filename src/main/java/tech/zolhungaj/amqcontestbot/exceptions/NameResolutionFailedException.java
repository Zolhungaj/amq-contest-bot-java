package tech.zolhungaj.amqcontestbot.exceptions;

public class NameResolutionFailedException extends RuntimeException{
    public NameResolutionFailedException(Throwable cause) {
        super(cause);
    }
}
