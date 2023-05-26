package tech.zolhungaj.amqcontestbot.chat;

import jakarta.annotation.PostConstruct;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tech.zolhungaj.amqapi.servercommands.gameroom.GameChatMessage;
import tech.zolhungaj.amqapi.servercommands.gameroom.GameChatUpdate;
import tech.zolhungaj.amqcontestbot.ApiManager;
import tech.zolhungaj.amqcontestbot.moderation.NameResolver;
import tech.zolhungaj.amqcontestbot.repository.PlayerService;

import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

/**Owner of all chat commands.
 * Chat commands are guaranteed to be executed in series, and should use blocking calls.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatCommands {
    /** Forbidden command prefixes.
     * A couple common emojis that start with the forward slash */
    private static final List<String> ILLEGAL_COMMAND_PREFIXES = List.of("o\\", "O\\", "0\\");
    private static final String UNKNOWN_COMMAND_I18N_NAME = "chat-commands.error.command-unknown";
    private final Map<String, Command> registeredCommands = new HashMap<>();

    private final Executor executor = Executors.newSingleThreadExecutor();

    private final ChatController chatController;
    private final NameResolver nameResolver;
    private final PlayerService playerService;
    private final ApiManager api;

    public void register(BiConsumer<String, List<String>> handler, String primaryCommandName, String... aliases){
        this.register(handler, primaryCommandName, List.of(aliases));
    }

    public void register(BiConsumer<String, List<String>> handler, String primaryCommandName, List<String> aliases){
        this.register(handler, Grant.NONE, primaryCommandName, aliases);
    }

    public void register(BiConsumer<String, List<String>> handler, Grant grant, String primaryCommandName, String... aliases){
        this.register(handler, grant, primaryCommandName, List.of(aliases));
    }

    public void register(BiConsumer<String, List<String>> handler, Grant grant, String primaryCommandName, List<String> aliases){
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
        Command command = new Command(primaryCommandName, grant, handler, aliases);
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
        List<String> arguments = new ArrayList<>(List.of(message.split(" +")));
        String commandName = arguments.remove(0);
        Command command = registeredCommands.get(commandName);
        if(command != null){
            executor.execute(new CommandHandler(command, sender, arguments));
        }else{
            chatController.send(UNKNOWN_COMMAND_I18N_NAME);
        }
    }

    @PostConstruct
    private void init(){
        api.on(GameChatMessage.class, this::handleMessage);
        api.on(GameChatUpdate.class, gameChatUpdate -> gameChatUpdate.messages().forEach(this::handleMessage));
        registerChatCommands();
    }

    private void registerChatCommands(){
        registerHelp();
        registerAlias();
    }

    private void registerHelp(){
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
    }

    private void help(){
        Set<Command> commands = new HashSet<>(registeredCommands.values());
        List<String> commandNames = commands.stream().map(Command::commandName).toList();
        chatController.send("command_list", String.join(", ", commandNames));
    }
    private void help(String commandName){
        Command command = registeredCommands.get(commandName);
        if(command != null){
            chatController.send(command.i18nCanonicalNameDescription(), command.commandName, command.aliasesToString());
            chatController.send(command.i18nCanonicalNameUsage(), command.commandName);
        }else{
            chatController.send(UNKNOWN_COMMAND_I18N_NAME);
        }
    }

    private void registerAlias(){
        register((sender, arguments) -> {
            if(arguments.size() == 1){
                String argument = arguments.get(0);
                listAliases(argument);
            }else{
                throw new IllegalArgumentException();
            }
        }, "alias");
    }

    private void listAliases(String commandName){
        Command command = registeredCommands.get(commandName);
        if(command != null){
            chatController.send("command_alias_list", command.commandName, command.aliasesToString());
        }else{
            chatController.send(UNKNOWN_COMMAND_I18N_NAME);
        }
    }

    @RequiredArgsConstructor
    private class CommandHandler implements Runnable{

        private final Command command;
        private final String sender;
        private final List<String> arguments;
        @Override
        public void run() {
            try{
                switch(command.grant()){
                    case NONE -> command.handler().accept(sender, arguments);
                    case MODERATOR -> {
                        String originalName = nameResolver.resolveOriginalName(sender);
                        if(playerService.isModerator(originalName) || playerService.isAdmin(originalName)){
                            command.handler().accept(sender, arguments);
                        }else{
                            throw new IllegalArgumentException("Must be moderator");
                        }
                    }
                    case ADMIN -> {
                        String originalName = nameResolver.resolveOriginalName(sender);
                        if(playerService.isAdmin(originalName)){
                            command.handler().accept(sender, arguments);
                        }else{
                            throw new IllegalArgumentException("Must be admin");
                        }
                    }
                }

            }catch(IllegalArgumentException e){
                chatController.send(command.i18nCanonicalNameUsage(), Objects.requireNonNullElse(e.getMessage(), ""));
            }
        }
    }

    private record Command(
            @NonNull String commandName,
            @NonNull Grant grant,
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

    public enum Grant{
        NONE,
        MODERATOR,
        ADMIN
    }
}
