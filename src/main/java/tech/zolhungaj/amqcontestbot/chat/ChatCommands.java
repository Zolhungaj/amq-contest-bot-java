package tech.zolhungaj.amqcontestbot.chat;

import lombok.NonNull;
import tech.zolhungaj.amqapi.AmqApi;
import tech.zolhungaj.amqapi.servercommands.gameroom.GameChatMessage;
import tech.zolhungaj.amqapi.servercommands.gameroom.GameChatUpdate;

//@Component //TODO: wrap api in something auto-wireable
public class ChatCommands {

    //TODO: database repositories

    public ChatCommands(AmqApi api){
        api.on(command -> {
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
