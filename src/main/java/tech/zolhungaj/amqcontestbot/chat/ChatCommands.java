package tech.zolhungaj.amqcontestbot.chat;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tech.zolhungaj.amqapi.servercommands.gameroom.GameChatMessage;
import tech.zolhungaj.amqapi.servercommands.gameroom.GameChatUpdate;
import tech.zolhungaj.amqcontestbot.ApiManager;

import java.util.*;
import java.util.function.BiConsumer;

@Slf4j
@Component
public class ChatCommands {
    /** Forbidden command prefixes.
     * A couple common emojis that start with the forward slash */
    private static final List<String> ILLEGAL_COMMAND_PREFIXES = List.of("o\\", "O\\", "0\\");
    private static final String UNKNOWN_COMMAND_I18N_NAME = "error_command_unknown";
    private final Map<String, Command> registeredCommands = new HashMap<>();

    private final ChatManager chatManager;

    public ChatCommands(@Autowired ApiManager api, @Autowired ChatManager chatManager){
        this.chatManager = chatManager;
        api.on(command -> {
            log.info("ChatCommand received: {}", command);
            if(command instanceof GameChatMessage gameChatMessage){
                this.handleMessage(gameChatMessage);
            }else if (command instanceof GameChatUpdate gameChatUpdate){
                gameChatUpdate.messages().forEach(this::handleMessage);
            }
            return true;
        });
        register((sender, arguments) -> {
            if(arguments.isEmpty()){
                help();
            } else if(arguments.size() == 1) {
                String argument = arguments.get(0);
                help(argument);
            } else{
                throw new IllegalArgumentException();
            }
        }, "help", "h");
        register((sender, arguments) -> {
            if(arguments.size() == 1){
                String argument = arguments.get(0);
                listAliases(argument);
            }else{
                throw new IllegalArgumentException();
            }
        }, "alias");
    }

    public void register(BiConsumer<String, List<String>> handler, String primaryCommandName, String... aliases){
        this.register(handler, primaryCommandName, List.of(aliases));
    }

    public void register(BiConsumer<String, List<String>> handler, String primaryCommandName, List<String> aliases){
        List<String> commandNames = new ArrayList<>();
        commandNames.add(primaryCommandName);
        commandNames.addAll(aliases);
        List<String> illegals = commandNames.stream()
                .filter(commandName -> ILLEGAL_COMMAND_PREFIXES.stream().anyMatch(commandName::startsWith))
                .toList();
        if(!illegals.isEmpty()){
            //prevent unreachable commands
            throw new IllegalArgumentException("Commands '" + String.join("', '", illegals) + "' are not valid command names, because they start with a forbidden prefix");
        }

        List<String> repeats = commandNames.stream().filter(registeredCommands::containsKey).toList();
        if(!repeats.isEmpty()){
            //prevent dumb mistakes in command naming
            throw new IllegalArgumentException("Commands '" + String.join("', '", repeats) + "' are already defined");
        }
        Command command = new Command(primaryCommandName, handler, aliases);
        commandNames.forEach(commandName -> registeredCommands.put(commandName, command));
    }

    private void handleMessage(@NonNull GameChatMessage message){
        if(message.message().startsWith("/")){
            String messageClean = message.message().substring(1);//remove /
            if(ILLEGAL_COMMAND_PREFIXES.stream().noneMatch(messageClean::startsWith)){
                handleCommand(messageClean, message.sender());
            }
        }
    }

    private void handleCommand(@NonNull String message, @NonNull String sender){
        List<String> splitCommand = new ArrayList<>(List.of(message.split(" +")));
        String commandName = splitCommand.remove(0);
        Command command = registeredCommands.get(commandName);
        if(command != null){
            try{
                command.handler().accept(sender, splitCommand);
            }catch(IllegalArgumentException e){
                chatManager.send(command.i18nCanonicalNameUsage(), e.getMessage());
            }
        }else{
            chatManager.send(UNKNOWN_COMMAND_I18N_NAME);
        }
    }

    private void help(){
        Set<Command> commands = new HashSet<>(registeredCommands.values());
        List<String> commandNames = commands.stream().map(Command::commandName).toList();
        chatManager.send("command_list", String.join(", ", commandNames));
    }
    private void help(String commandName){
        Command command = registeredCommands.get(commandName);
        if(command != null){
            chatManager.send(command.i18nCanonicalNameDescription(), command.commandName, command.aliasesToString());
            chatManager.send(command.i18nCanonicalNameUsage(), command.commandName);
        }else{
            chatManager.send(UNKNOWN_COMMAND_I18N_NAME);
        }
    }

    private void listAliases(String commandName){
        Command command = registeredCommands.get(commandName);
        if(command != null){
            chatManager.send("command_alias_list", command.commandName, command.aliasesToString());
        }else{
            chatManager.send(UNKNOWN_COMMAND_I18N_NAME);
        }
    }





    private record Command(
            @NonNull String commandName,
            @NonNull BiConsumer<String, List<String>> handler,
            @NonNull List<String> aliases){
        public String aliasesToString(){
            return String.join(", ", aliases);
        }
        public String i18nCanonicalName(){
            return "command_".concat(commandName);
        }
        public String i18nCanonicalNameDescription(){
            return i18nCanonicalName().concat("_description");
        }
        public String i18nCanonicalNameUsage(){
            return i18nCanonicalName().concat("_usage");
        }
    }
}
