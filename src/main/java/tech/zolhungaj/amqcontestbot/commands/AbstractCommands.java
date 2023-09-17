package tech.zolhungaj.amqcontestbot.commands;

import lombok.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

public abstract class AbstractCommands {
    /** Forbidden command prefixes.
     * A couple common emojis that start with the forward slash */
    protected static final List<String> ILLEGAL_COMMAND_PREFIXES = List.of("o\\", "O\\", "0\\");
    protected static final String UNKNOWN_COMMAND_I18N_NAME = "commands.error.command-unknown";
    protected final Map<String, Command> registeredCommands = new HashMap<>();

    private final Executor executor = Executors.newSingleThreadExecutor();

    protected void registerChatCommands(){
        registerHelp();
        registerAlias();
    }

    private void registerHelp(){
        register((sender, arguments) -> {
            if(arguments.isEmpty()){
                printCommandList(sender);
            } else if(arguments.size() == 1) {
                String argument = arguments.get(0);
                help(argument, sender);
            } else{
                throw new IllegalArgumentException();
            }
        }, "help", "h");
    }

    protected abstract void printCommandList(String sender);

    protected abstract void help(String argument, String sender);

    private void registerAlias(){
        register((sender, arguments) -> {
            if(arguments.size() == 1){
                String argument = arguments.get(0);
                listAliases(argument, sender);
            }else{
                throw new IllegalArgumentException();
            }
        }, "alias");
    }

    protected abstract void listAliases(String argument, String sender);

    public final void register(BiConsumer<String, List<String>> handler, String primaryCommandName, String... aliases){
        this.register(handler, primaryCommandName, List.of(aliases));
    }

    public final void register(BiConsumer<String, List<String>> handler, String primaryCommandName, List<String> aliases){
        this.register(handler, Grant.NONE, primaryCommandName, aliases);
    }

    public final void register(BiConsumer<String, List<String>> handler, Grant grant, String primaryCommandName, String... aliases){
        this.register(handler, grant, primaryCommandName, List.of(aliases));
    }

    public final void register(BiConsumer<String, List<String>> handler, Grant grant, String primaryCommandName, List<String> aliases){
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

    protected final void handleCommand(@NonNull String message, @NonNull String sender){
        List<String> arguments = new ArrayList<>(List.of(message.split(" +")));
        String commandName = arguments.remove(0);
        Command command = registeredCommands.get(commandName);
        if(command != null){
            executor.execute(createCommandHandler(command, sender, arguments));
        }else{
            unknownCommand(sender);
        }
    }

    protected abstract AbstractCommandHandler createCommandHandler(Command command, String sender, List<String> arguments);
    protected abstract void unknownCommand(String sender);
}
