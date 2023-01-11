package tech.zolhungaj.amqcontestbot.moderation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tech.zolhungaj.amqapi.servercommands.gameroom.GameChatMessage;
import tech.zolhungaj.amqapi.servercommands.gameroom.GameChatUpdate;
import tech.zolhungaj.amqcontestbot.ApiManager;

import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
@Component
public class ChatMonitor {

    private final Set<BannedPhrase> bannedPhrases;
    private final PlayerManager playerManager;

    public ChatMonitor(@Autowired ApiManager api, @Autowired PlayerManager playerManager){
        this.playerManager = playerManager;
        bannedPhrases = Set.of();
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

    private void handleMessage(GameChatMessage gameChatMessage){
        String message = gameChatMessage.message();
        for(BannedPhrase phrase : bannedPhrases){
            if(phrase.pattern.matcher(message).find()){
                playerManager.kick(gameChatMessage.sender());
                //TODO: report once API has it ready
            }
        }
    }

    private record BannedPhrase(Pattern pattern, String reason){

    }
}
