package tech.zolhungaj.amqcontestbot.chat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tech.zolhungaj.amqapi.servercommands.gameroom.GameChatMessage;
import tech.zolhungaj.amqapi.servercommands.gameroom.GameChatUpdate;
import tech.zolhungaj.amqcontestbot.ApiManager;

import java.time.Instant;
import java.util.function.Predicate;

@Component
public class UtilityCommands {

    private final ApiManager api;
    private final ChatCommands chatCommands;
    private final ChatController chatController;

    public UtilityCommands(@Autowired ApiManager api,
                           @Autowired ChatCommands chatCommands,
                           @Autowired ChatController chatController){
        this.api = api;
        this.chatCommands = chatCommands;
        this.chatController = chatController;
        registerPing();
    }

    private void registerPing(){
        chatCommands.register((sender, arguments) -> {
            long ping = api.getPing();
            String sentMessage = chatController.send("ping.base", ping).get(0);
            Instant sendInstant = Instant.now();
            long sendInstantAsMillis = sendInstant.toEpochMilli();
            Predicate<GameChatMessage> printWhenMatch = gameChatMessage -> {
                if(gameChatMessage.sender().equals(api.getSelfName())
                        && gameChatMessage.message().equals(sentMessage)){
                    Instant receiveInstant = Instant.now();
                    long receiveInstantAsMillis = receiveInstant.toEpochMilli();
                    long difference = receiveInstantAsMillis - sendInstantAsMillis;
                    chatController.send("ping.chat", difference);
                    return true;
                }
                return false;
            };
            api.once(command -> {
                if(command instanceof GameChatMessage gameChatMessage){
                    return printWhenMatch.test(gameChatMessage);
                }else if(command instanceof GameChatUpdate gameChatUpdate){
                    return gameChatUpdate.messages().stream().anyMatch(printWhenMatch);
                }
                return false;
            });
        }, "ping");
    }
}
