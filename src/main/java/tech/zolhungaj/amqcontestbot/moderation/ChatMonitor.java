package tech.zolhungaj.amqcontestbot.moderation;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tech.zolhungaj.amqapi.servercommands.gameroom.GameChatMessage;
import tech.zolhungaj.amqapi.servercommands.gameroom.GameChatUpdate;
import tech.zolhungaj.amqapi.servercommands.gameroom.NewPlayer;
import tech.zolhungaj.amqapi.servercommands.gameroom.SpectatorJoined;
import tech.zolhungaj.amqcontestbot.ApiManager;

import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMonitor {

    private final ApiManager api;
    private final NameResolver nameResolver;
    private final PunishmentManager punishmentManager;
    private Set<BannedPhrase> bannedPhrases;

    @PostConstruct
    private void init(){
        bannedPhrases = Set.of();
        api.on(GameChatMessage.class, this::handleMessage);
        api.on(GameChatUpdate.class, gameChatUpdate -> gameChatUpdate.messages().forEach(this::handleMessage));
        api.on(NewPlayer.class, newPlayer -> handlePlayer(newPlayer.playerName()));
        api.on(SpectatorJoined.class, spectatorJoined -> handlePlayer(spectatorJoined.playerName()));
        //TODO: check answers too
    }

    private void handleMessage(GameChatMessage gameChatMessage){
        rateMessage(gameChatMessage.sender(), gameChatMessage.message());
    }

    private void handlePlayer(String nickname){
        rateName(nickname, nickname);
        nameResolver.resolveOriginalNameAsync(nickname)
                .thenAccept(originalName -> rateName(nickname, originalName));
    }

    private void rateMessage(String sender, String message){
        Set<String> reasons = findBannedPhrases(message);
        if(!reasons.isEmpty()){
            punishmentManager.kick(sender);
        }
        //reasons.forEach();
        //TODO: report for each violation in message
    }

    private void rateName(String nickname, String name){
        Set<String> reasons = findBannedPhrases(name);
        if(!reasons.isEmpty()){
            punishmentManager.kick(nickname);
            //TODO: report for "Offensive Name"
        }
    }

    private Set<String> findBannedPhrases(String content){
        return bannedPhrases.stream()
                .filter(phrase -> phrase.pattern.matcher(content).find())
                .map(BannedPhrase::reason)
                .collect(Collectors.toSet());
    }

    private record BannedPhrase(Pattern pattern, String reason){

    }
}
