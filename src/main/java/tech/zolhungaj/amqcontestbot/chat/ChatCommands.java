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

@Component
@Slf4j
public class ChatCommands {

    private final Map<String, BiConsumer<String, List<String>>> registeredCommands = new HashMap<>();

    public ChatCommands(@Autowired ApiManager api){
        api.on(command -> {
            log.info("ChatCommand received: {}", command);
            if(command instanceof GameChatMessage gameChatMessage){
                this.handleMessage(gameChatMessage);
            }else if (command instanceof GameChatUpdate gameChatUpdate){
                gameChatUpdate.messages().forEach(this::handleMessage);
            }
            return true;
        });
    }

    public void register(BiConsumer<String, List<String>> handler, String... commandNames){
        this.register(handler, List.of(commandNames));
    }

    public void register(BiConsumer<String, List<String>> handler, Collection<String> commandNames){
        if(commandNames.isEmpty()){
            throw new IllegalArgumentException("Command must have at least one name");
        }
        List<String> repeats = commandNames.stream().filter(registeredCommands::containsKey).toList();
        if(!repeats.isEmpty()){
            //prevent dumb mistakes in command naming
            throw new IllegalArgumentException("Commands '" + String.join("', '") + "' are already defined");
        }
        commandNames.forEach(commandName -> registeredCommands.put(commandName, handler));
    }

    private void handleMessage(@NonNull GameChatMessage message){
        if(message.message().startsWith("/")){
            handleCommand(message.message(), message.sender());
        }
    }

    private void handleCommand(@NonNull String message, @NonNull String sender){
        message = message.substring(1);//remove /
        List<String> splitCommand = new ArrayList<>(List.of(message.split(" +")));
        String command = splitCommand.remove(0);
        BiConsumer<String, List<String>> handler = registeredCommands.get(command);
        if(handler != null){
            handler.accept(sender, splitCommand);
        }else{
            //TODO: send message via ChatManager
        }
    }
}
