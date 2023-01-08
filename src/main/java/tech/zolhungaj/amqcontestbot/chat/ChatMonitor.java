package tech.zolhungaj.amqcontestbot.chat;

import tech.zolhungaj.amqapi.AmqApi;
import tech.zolhungaj.amqapi.servercommands.gameroom.GameChatMessage;
import tech.zolhungaj.amqapi.servercommands.gameroom.GameChatUpdate;

public class ChatMonitor {

    public ChatMonitor(AmqApi api){
        api.on(command -> {
            if(command instanceof GameChatMessage gameChatMessage){
                this.handleMessage(gameChatMessage);
            }else if (command instanceof GameChatUpdate gameChatUpdate){
                gameChatUpdate.messages().forEach(this::handleMessage);
            }
            return true;
        });
    }

    private void handleMessage(GameChatMessage message){

    }
}
