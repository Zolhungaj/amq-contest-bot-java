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
import tech.zolhungaj.amqcontestbot.database.service.TeamService;
import tech.zolhungaj.amqcontestbot.gamemode.GameMode;
import tech.zolhungaj.amqcontestbot.moderation.NameResolver;
import tech.zolhungaj.amqcontestbot.room.lobby.LobbyStateManager;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameManager {
    private final ApiManager api;
    private final SongService songService;
    private final GameService gameService;
    private final PlayerService playerService;
    private final TeamService teamService;
    private final LobbyStateManager lobbyStateManager;
    private final NameResolver nameResolver;
    private final Map<Integer, GameContestant> contestants = new HashMap<>();
    private final Map<Integer, GameContestantEntity> databaseContestants = new HashMap<>();

    /** map over players contained in contestants, used to update disconnects*/
    private final Map<Integer, PlayerInformation> players = new HashMap<>();
    private final Map<Integer, Duration> playerAnswerTimes = new HashMap<>();
    private final Map<Integer, String> playerAnswers = new HashMap<>();
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
                    .forEach(answer -> {
                        playerAnswerTimes.putIfAbsent(answer.gamePlayerId(), Duration.between(roundStartTime, now));
                        playerAnswers.put(answer.gamePlayerId(), answer.answer());
                    });
        });
    }

    private void startGame(GameStarting info){
        reset();
        currentGameMode = lobbyStateManager.getGameMode();
        currentGame = gameService.startGame(currentGameMode.ruleset(), currentGameMode.scoringType(), currentGameMode.teamSize());
        if(currentGameMode.teamSize() == 1){
            info.players()
                    .stream()
                    .map(quizPlayer -> new ContestantPlayer(quizPlayer.gamePlayerId(), quizPlayer.playerName()))
                    .forEach( contestant -> {
                contestants.put(contestant.getGamePlayerId(), contestant);
                players.put(contestant.getGamePlayerId(), contestant);
                String nickname = contestant.getPlayerName();
                String playerOriginalName = nameResolver.resolveOriginalName(nickname);
                PlayerEntity player = playerService.getPlayer(playerOriginalName).orElseThrow();
                PlayerContestantEntity contestantEntity = player.getContestant();
                assert contestantEntity != null;
                GameContestantEntity gameContestantEntity = gameService.createGameContestant(currentGame, contestantEntity);
                databaseContestants.put(contestant.getGamePlayerId(), gameContestantEntity);
            });
        }else{
            Map<Integer, List<TeamPlayer>> teams = new HashMap<>();
            info.players()
                    .stream()
                    .map(quizPlayer -> new TeamPlayer(quizPlayer.gamePlayerId(), quizPlayer.playerName(), quizPlayer.teamNumber().orElseThrow()))
                    .forEach(teamPlayer -> {
                teams.putIfAbsent(teamPlayer.getTeamNumber(), new ArrayList<>());
                teams.get(teamPlayer.getTeamNumber())
                        .add(teamPlayer);
                players.put(teamPlayer.getGamePlayerId(), teamPlayer);
            });
            teams.keySet().forEach(teamNumber -> {
                List<TeamPlayer> teamPlayers = teams.get(teamNumber);
                ContestantTeam contestantTeam = new ContestantTeam(teamNumber, teamPlayers);
                contestants.put(teamNumber, contestantTeam);

                List<PlayerEntity> playerEntities = teamPlayers.stream()
                        .map(TeamPlayer::getPlayerName)
                        .map(nameResolver::resolveOriginalName)
                        .map(playerService::getPlayer)
                        .map(Optional::orElseThrow)
                        .toList();
                TeamEntity team = teamService.getOrCreateTeam(playerEntities);
                GameContestantEntity gameTeamEntity = gameService.createGameContestant(currentGame, team.getContestant());
                databaseContestants.put(teamNumber, gameTeamEntity);
            });
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
        contestants.keySet().forEach(id -> {
            GameContestant contestant = contestants.get(id);
            GameContestantEntity contestantEntity = databaseContestants.get(id);
            assert contestantEntity != null;
            contestantEntity.updateFromGameContestant(contestant);
        });
        gameService.updateGameContestants(databaseContestants.values());
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
        updateGameContestants();

        //discard once they are recorded
        playerAnswerTimes.clear();
        playerAnswers.clear();
    }

    private void recordAnswerResultsPerPlayer(PlayerAnswerResult answerResult, GameSongEntity gameSong){
        GameContestant gameContestant = contestants.get(answerResult.gamePlayerId());
        GameContestantEntity databaseContestant = databaseContestants.get(answerResult.gamePlayerId());
        if(gameContestant == null){
            log.error("gamePlayerId not in contestants {}, {}", answerResult.gamePlayerId(), answerResult);
            return;
        }
        Duration playerAnswerTime = playerAnswerTimes.get(answerResult.gamePlayerId());
        gameService.createGameAnswer(gameSong, databaseContestant.getContestant(), answerResult.correct(), playerAnswers.get(answerResult.gamePlayerId()), playerAnswerTime);
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
