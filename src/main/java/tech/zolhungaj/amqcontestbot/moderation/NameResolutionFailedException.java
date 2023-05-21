package tech.zolhungaj.amqcontestbot.moderation;

public class NameResolutionFailedException extends RuntimeException{
    public NameResolutionFailedException(Throwable cause) {
        super(cause);
    }
}
