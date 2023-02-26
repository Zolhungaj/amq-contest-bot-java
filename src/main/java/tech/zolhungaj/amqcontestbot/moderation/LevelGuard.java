package tech.zolhungaj.amqcontestbot.moderation;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tech.zolhungaj.amqapi.clientcommands.lobby.Kick;
import tech.zolhungaj.amqapi.servercommands.gameroom.NewPlayer;
import tech.zolhungaj.amqapi.servercommands.gameroom.lobby.SpectatorChangedToPlayer;
import tech.zolhungaj.amqcontestbot.ApiManager;
import tech.zolhungaj.amqcontestbot.Util;
import tech.zolhungaj.amqcontestbot.chat.ChatController;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@ConditionalOnProperty(prefix = "bot", name = "level-guard.enabled", havingValue = "true")
@RequiredArgsConstructor
public class LevelGuard {
    private final ApiManager api;
    private final ChatController chatController;
    @Value("${bot.level-guard.allow-guests:true}")
    private boolean allowGuests;
    @Value("${bot.level-guard.min-level:0}")
    private int minLevel;
    @Value("${bot.level-guard.max-level:99999}")
    private int maxLevel;
    @Value("${bot.level-guard.warning-limit:3}")
    private int warningLimit;
    private final ConcurrentMap<String, Integer> issuedWarnings = new ConcurrentHashMap<>();

    @PostConstruct
    public void init(){
        api.on(command -> {
            if(command instanceof NewPlayer player){
                onJoin(player.playerName(), player.level());
            }
            if(command instanceof SpectatorChangedToPlayer player){
                onJoin(player.getPlayerName(), player.getLevel());
            }
            return true;
        });
    }

    private void onJoin(String playerName, int level){
        if(!allowGuests && Util.isGuest(playerName)){
            handleViolation(playerName, Violation.GUEST);
        }else if(level < minLevel){
            handleViolation(playerName, Violation.MIN_LEVEL);
        }else if(level > maxLevel){
            handleViolation(playerName, Violation.MAX_LEVEL);
        }
    }

    private void handleViolation(String playerName, Violation violation){
        int warningsIssued = issueWarning(playerName);
        if(warningsIssued > warningLimit){
            api.sendCommand(new Kick(playerName));
            return;
        }
        switch(violation){
            case GUEST -> chatController.send("level-guard.warn.guest", playerName, warningsIssued, warningLimit);
            case MIN_LEVEL -> chatController.send("level-guard.warn.min-level", playerName, warningsIssued, warningLimit, minLevel);
            case MAX_LEVEL -> chatController.send("level-guard.warn.max-level", playerName, warningsIssued, warningLimit, maxLevel);
        }
    }

    private int issueWarning(String playerName){
        issuedWarnings.putIfAbsent(playerName, 0);
        int count = issuedWarnings.get(playerName) + 1;
        issuedWarnings.put(playerName, count);
        return count;
    }

    private enum Violation{
        GUEST,
        MIN_LEVEL,
        MAX_LEVEL
    }
}
