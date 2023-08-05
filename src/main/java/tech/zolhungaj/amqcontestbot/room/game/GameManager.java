package tech.zolhungaj.amqcontestbot.room.game;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tech.zolhungaj.amqapi.servercommands.gameroom.game.*;
import tech.zolhungaj.amqapi.servercommands.globalstate.LoginComplete;
import tech.zolhungaj.amqapi.servercommands.objects.PlayerAnswerResult;
import tech.zolhungaj.amqcontestbot.ApiManager;
import tech.zolhungaj.amqcontestbot.database.model.*;
import tech.zolhungaj.amqcontestbot.database.service.GameService;
import tech.zolhungaj.amqcontestbot.database.service.PlayerService;
import tech.zolhungaj.amqcontestbot.database.service.SongService;
import tech.zolhungaj.amqcontestbot.gamemode.GameMode;
import tech.zolhungaj.amqcontestbot.moderation.NameResolver;
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
    private final GameService gameService;
    private final PlayerService playerService;
    private final LobbyStateManager lobbyStateManager;
    private final NameResolver nameResolver;
    private final Map<Integer, GameContestant> contestants = new HashMap<>();
    private final Map<Integer, GameContestantEntity> databaseContestants = new HashMap<>();

    /** map over players contained in contestants, used to update disconnects*/
    private final Map<Integer, PlayerInformation> players = new HashMap<>();
    private final Map<Integer, Duration> playerAnswerTimes = new HashMap<>();
    private GameEntity currentGame;
    private GameMode currentGameMode;
    private Instant roundStartTime;

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
            if(currentGame != null){
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
        currentGameMode = lobbyStateManager.getGameMode();
        currentGame = gameService.startGame(currentGameMode.ruleset(), currentGameMode.scoringType(), currentGameMode.teamSize());
        if(currentGameMode.teamSize() == 1){
            info.players().forEach(quizPlayer -> {
                ContestantPlayer contestant = new ContestantPlayer(quizPlayer.gamePlayerId(), quizPlayer.playerName());
                contestants.put(quizPlayer.gamePlayerId(), contestant);
                players.put(quizPlayer.gamePlayerId(), contestant);
                String nickname = quizPlayer.playerName();
                String playerOriginalName = nameResolver.resolveOriginalName(nickname);
                PlayerEntity player = playerService.getPlayer(playerOriginalName).orElseThrow();
                PlayerContestantEntity contestantEntity = player.getContestant();
                assert contestantEntity != null;
                GameContestantEntity gameContestantEntity = gameService.createGameContestant(currentGame, contestantEntity);
                databaseContestants.put(quizPlayer.gamePlayerId(), gameContestantEntity);
            });
        }else{
            //TODO: teams
        }
    }

    private void finishGame(){
        if(currentGame == null){
            return;
        }
        currentGameMode.rank(contestants.values());
        updateGameContestants();
        gameService.finishGame(currentGame);
        reset();
    }

    private void updateGameContestants(){
        //TODO
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
        if(currentGame == null){
            return;
        }
        GameSongEntity gameSongEntity = gameService.createGameSong(currentGame, songEntity);
        if(currentGameMode.teamSize() == 1){
            answerResults.players().forEach(player -> recordAnswerResultsPerPlayer(player, gameSongEntity));
        }else{
            recordAnswerResultsPerTeam(answerResults.players(), gameSongEntity);
        }
    }

    private void recordAnswerResultsPerPlayer(PlayerAnswerResult answerResult, GameSongEntity gameSong){
        GameContestant gameContestant = contestants.get(answerResult.gamePlayerId());
        GameContestantEntity databaseContestant = databaseContestants.get(answerResult.gamePlayerId());
        if(gameContestant == null){
            log.error("gamePlayerId not in contestants {}, {}", answerResult.gamePlayerId(), answerResult);
            return;
        }
        Duration playerAnswerTime = playerAnswerTimes.get(answerResult.gamePlayerId());
        lobbyStateManager.getGameMode().score(gameContestant, answerResult, playerAnswerTime);
    }

    private void recordAnswerResultsPerTeam(List<PlayerAnswerResult> answerResults, GameSongEntity gameSong){
        //TODO
    }

    private void reset(){
        contestants.clear();
        databaseContestants.clear();
        players.clear();
        playerAnswerTimes.clear();
        currentGame = null;
        currentGameMode = null;
    }
}
