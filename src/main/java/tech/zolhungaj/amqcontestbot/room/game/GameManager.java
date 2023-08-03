package tech.zolhungaj.amqcontestbot.room.game;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tech.zolhungaj.amqapi.servercommands.gameroom.game.*;
import tech.zolhungaj.amqapi.servercommands.globalstate.LoginComplete;
import tech.zolhungaj.amqapi.servercommands.objects.PlayerAnswerResult;
import tech.zolhungaj.amqcontestbot.ApiManager;
import tech.zolhungaj.amqcontestbot.database.model.SongEntity;
import tech.zolhungaj.amqcontestbot.database.service.SongService;
import tech.zolhungaj.amqcontestbot.room.lobby.LobbyStateManager;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameManager {
    private final ApiManager api;
    private final SongService songService;
    private final LobbyStateManager lobbyStateManager;
    private final Map<Integer, GameContestant> contestants = new HashMap<>();

    /** map over players contained in contestants, used to update disconnects*/
    private final Map<Integer, PlayerInformation> players = new HashMap<>();
    private final Map<Integer, Duration> playerAnswerTimes = new HashMap<>();

    private Instant roundStartTime;
    private boolean inGame = false;
    private Mode mode = Mode.PLAYER;//TODO hook up

    private enum Mode{
        TEAM,
        PLAYER
    }

    @PostConstruct
    private void init(){
        api.on(LoginComplete.class, loginComplete -> reset());
        api.on(GameStarting.class, this::startGame);
        //TODO:game end, including incorrect end
        //TODO:disconnect
        api.on(AnswerResults.class, this::answerResults);
        api.on(PlayNextSong.class, playNextSong -> roundStartTime = Instant.now());
        api.on(PlayersAnswered.class, playersAnswered -> {
            Instant now = Instant.now();
            if(inGame){
                playersAnswered.gamePlayerIds().forEach(gamePlayerId -> {
                    if(players.containsKey(gamePlayerId)){
                        playerAnswerTimes.put(gamePlayerId, Duration.between(roundStartTime, now));
                    }else{
                        log.error("Unknown gamePlayerId {}", gamePlayerId);
                    }
                });
            }
        });
        api.on(AnswerReveal.class, answerReveal -> {
            Instant now = Instant.now();
            //as a backup, anyone who has an answer at this point but no answer time, gets the round time
            answerReveal
                    .answers()
                    .stream()
                    .filter(answer -> answer.answer() != null && !answer.answer().isBlank())
                    .forEach(answer -> playerAnswerTimes.putIfAbsent(answer.gamePlayerId(), Duration.between(roundStartTime, now)));
        });
    }

    private void startGame(GameStarting info){
        reset();
        inGame = true;
    }

    private void handleDisconnect(int gamePlayerId, boolean disconnected){
        PlayerInformation playerInformation = players.get(gamePlayerId);
        if(playerInformation != null){
            playerInformation.setDisconnected(disconnected);
        }else{
            log.error("Unknown gamePlayerId {}", gamePlayerId);
        }
    }

    private void answerResults(AnswerResults answerResults){
        //songs are worth tracking regardless of whether a game is in progress
        SongEntity songEntity = songService.updateAndGetSongEntityFromSongInfo(answerResults.songInfo());
        if(!inGame){
            return;
        }
        switch (mode){
            case PLAYER -> answerResults.players().forEach(player -> recordAnswerResultsPerPlayer(player, songEntity));
            case TEAM -> recordAnswerResultsPerTeam(answerResults.players(), songEntity);
        }
    }

    private void recordAnswerResultsPerPlayer(PlayerAnswerResult answerResult, SongEntity songEntity){
        GameContestant gameContestant = contestants.get(answerResult.gamePlayerId());
        if(gameContestant == null){
            log.error("gamePlayerId not in contestants {}, {}", answerResult.gamePlayerId(), answerResult);
            return;
        }
        Duration playerAnswerTime = playerAnswerTimes.get(answerResult.gamePlayerId());
        lobbyStateManager.getGameMode().score(gameContestant, answerResult, playerAnswerTime);
    }

    private void recordAnswerResultsPerTeam(List<PlayerAnswerResult> answerResults, SongEntity songEntity){
        //TODO
    }

    private void reset(){
        contestants.clear();
        players.clear();
        playerAnswerTimes.clear();
        inGame = false;
    }
}
