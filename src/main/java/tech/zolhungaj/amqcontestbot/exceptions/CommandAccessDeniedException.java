package tech.zolhungaj.amqcontestbot.exceptions;

import tech.zolhungaj.amqcontestbot.commands.Grant;

public class CommandAccessDeniedException extends IncorrectCommandUsageException{
    public CommandAccessDeniedException(String sender, String commandName, Grant grant) {
        super("chat-command.access-denied", sender, commandName, grant.name());
    }
}
