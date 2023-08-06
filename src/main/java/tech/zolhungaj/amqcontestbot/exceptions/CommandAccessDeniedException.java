package tech.zolhungaj.amqcontestbot.exceptions;

import tech.zolhungaj.amqcontestbot.chat.ChatCommands;

public class CommandAccessDeniedException extends IncorrectCommandUsageException{
    public CommandAccessDeniedException(String sender, String commandName, ChatCommands.Grant grant) {
        super("chat-command.access-denied", sender, commandName, grant.name());
    }
}
