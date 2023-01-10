package tech.zolhungaj.amqcontestbot.chat;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tech.zolhungaj.amqapi.servercommands.gameroom.GameChatMessage;
import tech.zolhungaj.amqapi.servercommands.gameroom.GameChatUpdate;
import tech.zolhungaj.amqcontestbot.ApiManager;

@Slf4j
@Component
public class ChatMonitor {

    public ChatMonitor(@Autowired ApiManager api){
        api.on(command -> {
            log.info("ChatMonitor received {}", command);
            if(command instanceof GameChatMessage gameChatMessage){
                this.handleMessage(gameChatMessage);
            }else if (command instanceof GameChatUpdate gameChatUpdate){
                gameChatUpdate.messages().forEach(this::handleMessage);
            }
            return true;
        });
    }

    private void handleMessage(GameChatMessage message){
        //TODO: add filtering etc.
    }
}
