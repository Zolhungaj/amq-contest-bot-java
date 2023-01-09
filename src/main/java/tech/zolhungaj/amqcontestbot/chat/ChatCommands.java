package tech.zolhungaj.amqcontestbot.chat;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tech.zolhungaj.amqapi.servercommands.gameroom.GameChatMessage;
import tech.zolhungaj.amqapi.servercommands.gameroom.GameChatUpdate;
import tech.zolhungaj.amqcontestbot.ApiManager;

@Component
@Slf4j
public class ChatCommands {

    //TODO: database repositories

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

    private void handleMessage(@NonNull GameChatMessage message){
        if(message.message().startsWith("/")){
            handleCommand(message.message(), message.sender());
        }
    }

    private void handleCommand(@NonNull String message, @NonNull String sender){
        //TODO: handle command
    }
}
