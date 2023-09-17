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
import tech.zolhungaj.amqcontestbot.exceptions.CommandAccessDeniedException;
import tech.zolhungaj.amqcontestbot.exceptions.IncorrectCommandUsageException;
import tech.zolhungaj.amqcontestbot.moderation.NameResolver;

import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

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
    private final ModerationService moderationService;
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
            executor.execute(new ChatCommandHandler(command, sender, arguments));
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
                printCommandList(sender);
            } else if(arguments.size() == 1) {
                String argument = arguments.get(0);
                help(argument);
            } else{
                throw new IllegalArgumentException();
            }
        }, "help", "h");
    }

    private void printCommandList(String sender){
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
    private void help(String commandName){
        Command command = registeredCommands.get(commandName);
        if(command != null){
            chatController.send(command.i18nCanonicalNameDescription(), command.commandName(), command.aliasesToString());
            chatController.send(command.i18nCanonicalNameUsage(), command.commandName());
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
            chatController.send("chat-commands.alias", command.commandName(), command.aliasesToString());
        }else{
            chatController.send(UNKNOWN_COMMAND_I18N_NAME);
        }
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
