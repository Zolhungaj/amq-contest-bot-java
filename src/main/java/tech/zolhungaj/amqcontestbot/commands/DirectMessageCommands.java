package tech.zolhungaj.amqcontestbot.commands;

import jakarta.annotation.PostConstruct;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tech.zolhungaj.amqapi.servercommands.social.DirectMessage;
import tech.zolhungaj.amqcontestbot.ApiManager;
import tech.zolhungaj.amqcontestbot.chat.DirectMessageController;
import tech.zolhungaj.amqcontestbot.database.service.ModerationService;
import tech.zolhungaj.amqcontestbot.moderation.NameResolver;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**Owner of all chat commands.
 * Chat commands are guaranteed to be executed in series, and should use blocking calls.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DirectMessageCommands extends AbstractCommands{
    private final DirectMessageController dmController;
    private final NameResolver nameResolver;
    private final ModerationService moderationService;
    private final ApiManager api;

    @PostConstruct
    private void init(){
        api.on(DirectMessage.class, this::handleMessage);
        super.registerChatCommands();
    }


    private void handleMessage(@NonNull DirectMessage message){
        if(message.message().startsWith("/")){
            String messageClean = message.message().substring(1);//remove /
            if(ILLEGAL_COMMAND_PREFIXES.stream().noneMatch(messageClean::startsWith)){
                handleCommand(messageClean, message.sender());
            }
        }
    }

    @Override
    protected void printCommandList(String sender){
        Set<Command> commands = new HashSet<>(registeredCommands.values());
        List<String> commandsSorted = commands.stream().map(Command::commandName).sorted(String::compareToIgnoreCase).toList();
        dmController.send(sender, "dm-commands.help.all",  String.join(", ", commandsSorted));
    }
    @Override
    protected void help(String commandName, String sender){
        Command command = registeredCommands.get(commandName);
        if(command != null){
            dmController.send(sender, command.i18nCanonicalNameDescription(), command.commandName(), command.aliasesToString());
            dmController.send(sender, command.i18nCanonicalNameUsage(), command.commandName());
        }else{
            unknownCommand(sender);
        }
    }


    @Override
    protected void listAliases(String commandName, String sender){
        Command command = registeredCommands.get(commandName);
        if(command != null){
            dmController.send(sender, "chat-commands.alias", command.commandName(), command.aliasesToString());
        }else{
            unknownCommand(sender);
        }
    }

    @Override
    protected AbstractCommandHandler createCommandHandler(Command command, String sender, List<String> arguments) {
        return new DirectMessageCommandHandler(command, sender, arguments);
    }

    @Override
    protected void unknownCommand(String sender) {
        dmController.send(sender, UNKNOWN_COMMAND_I18N_NAME);
    }


    private class DirectMessageCommandHandler extends AbstractCommandHandler{
        public DirectMessageCommandHandler(Command command, String sender, List<String> arguments) {
            super(command, sender, arguments, moderationService, nameResolver);
        }

        @Override
        protected void handleIncorrectCommandUsage(String i18nIdentifier, List<String> arguments) {
            dmController.send(this.sender, i18nIdentifier, arguments.toArray());
        }

        @Override
        protected void handleIllegalArgumentException(String i18nCanonicalNameUsage) {
            dmController.send(this.sender, i18nCanonicalNameUsage);
        }
    }

}
