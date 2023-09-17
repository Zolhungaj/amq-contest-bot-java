package tech.zolhungaj.amqcontestbot.commands;

import jakarta.annotation.PostConstruct;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tech.zolhungaj.amqapi.servercommands.gameroom.GameChatMessage;
import tech.zolhungaj.amqapi.servercommands.gameroom.GameChatUpdate;
import tech.zolhungaj.amqcontestbot.ApiManager;
import tech.zolhungaj.amqcontestbot.chat.ChatController;
import tech.zolhungaj.amqcontestbot.database.service.ModerationService;
import tech.zolhungaj.amqcontestbot.moderation.NameResolver;

import java.util.*;

/**Owner of all chat commands.
 * Chat commands are guaranteed to be executed in series, and should use blocking calls.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatCommands extends AbstractCommands{


    private final ChatController chatController;
    private final NameResolver nameResolver;
    private final ModerationService moderationService;
    private final ApiManager api;

    @PostConstruct
    private void init(){
        api.on(GameChatMessage.class, this::handleMessage);
        api.on(GameChatUpdate.class, gameChatUpdate -> gameChatUpdate.messages().forEach(this::handleMessage));
        super.registerChatCommands();
    }


    private void handleMessage(@NonNull GameChatMessage message){
        if(message.message().startsWith("/")){
            String messageClean = message.message().substring(1);//remove /
            if(ILLEGAL_COMMAND_PREFIXES.stream().noneMatch(messageClean::startsWith)){
                handleCommand(messageClean, message.sender());
            }
        }
    }

    @Override
    protected void printCommandList(String sender){
        String originalName = nameResolver.resolveOriginalName(sender);
        Set<Command> commands = new HashSet<>(registeredCommands.values());
        Map<Grant, List<String>> commandsByGrant = new EnumMap<>(Grant.class);
        commands.forEach(command -> commandsByGrant.computeIfAbsent(command.grant(), grant -> new ArrayList<>()).add(command.commandName()));
        commandsByGrant.replaceAll((grant, commandNames) -> commandNames.stream().sorted(String::compareToIgnoreCase).toList());
        if(commandsByGrant.containsKey(Grant.NONE)){
            chatController.send("chat-commands.help.common", String.join(", ", commandsByGrant.get(Grant.NONE)));
        }
        if(commandsByGrant.containsKey(Grant.MODERATOR) && moderationService.isModerator(originalName)){
            chatController.send("chat-commands.help.moderator", String.join(", ", commandsByGrant.get(Grant.MODERATOR)));
        }
        if(commandsByGrant.containsKey(Grant.ADMIN) && moderationService.isAdmin(originalName)){
            chatController.send("chat-commands.help.admin", String.join(", ", commandsByGrant.get(Grant.ADMIN)));
        }
        if(commandsByGrant.containsKey(Grant.OWNER) && moderationService.isOwner(originalName)){
            chatController.send("chat-commands.help.owner", String.join(", ", commandsByGrant.get(Grant.OWNER)));
        }
    }

    @Override
    protected void help(String commandName, String sender){
        Command command = registeredCommands.get(commandName);
        if(command != null){
            chatController.send(command.i18nCanonicalNameDescription(), command.commandName(), command.aliasesToString());
            chatController.send(command.i18nCanonicalNameUsage(), command.commandName());
        }else{
            unknownCommand(sender);
        }
    }

    @Override
    protected final void listAliases(String commandName, String sender){
        Command command = registeredCommands.get(commandName);
        if(command != null){
            chatController.send("chat-commands.alias", command.commandName(), command.aliasesToString());
        }else{
            unknownCommand(sender);
        }
    }

    @Override
    protected AbstractCommandHandler createCommandHandler(Command command, String sender, List<String> arguments) {
        return new ChatCommandHandler(command, sender, arguments);
    }

    @Override
    protected void unknownCommand(String sender) {
        chatController.send(UNKNOWN_COMMAND_I18N_NAME, sender);
    }


    private class ChatCommandHandler extends AbstractCommandHandler{
        public ChatCommandHandler(Command command, String sender, List<String> arguments) {
            super(command, sender, arguments, moderationService, nameResolver);
        }

        @Override
        protected void handleIncorrectCommandUsage(String i18nIdentifier, List<String> arguments) {
            chatController.send(i18nIdentifier, arguments.toArray());
        }

        @Override
        protected void handleIllegalArgumentException(String i18nCanonicalNameUsage) {
            chatController.send(i18nCanonicalNameUsage);
        }
    }

}
