package tech.zolhungaj.amqcontestbot.room.game;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tech.zolhungaj.amqapi.servercommands.globalstate.LoginComplete;
import tech.zolhungaj.amqcontestbot.ApiManager;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameManager {
    private final ApiManager api;
    private final Map<Integer, GameEntity> contestants = new HashMap<>();

    /** map over players contained in contestants, used to update disconnects*/
    private final Map<Integer, PlayerInformation> players = new HashMap<>();
    private final Map<Integer, Long> playerAnswerTimes = new HashMap<>();
    private String selfName = "";
    private boolean inGame = false;

    @PostConstruct
    private void init(){
        api.on(command -> {
            if(command instanceof LoginComplete loginComplete){
                this.selfName = loginComplete.selfName();
                reset();
            }
            /*
            TODO: game start
            game end, including incorrect end
            round start
            answer time
            disconnect*/
            return true;
        });
    }

    private void startGame(Object info){

    }

    private void handleDisconnect(int gamePlayerId, boolean disconnected){
        PlayerInformation playerInformation = players.get(gamePlayerId);
        if(playerInformation != null){
            playerInformation.setDisconnected(disconnected);
        }else{
            log.error("Unknown gamePlayerId {}", gamePlayerId);
        }
    }

    private void reset(){
        contestants.clear();
        players.clear();
        playerAnswerTimes.clear();
        inGame = false;
    }
}
