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
import tech.zolhungaj.amqcontestbot.database.service.*;
import tech.zolhungaj.amqcontestbot.gamemode.AnswerResult;
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
    private final ContestantService contestantService;
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
                        if(currentGameMode.teamSize() == 1){
                            playerAnswerTimes.putIfAbsent(answer.gamePlayerId(), Duration.between(roundStartTime, now));
                        } //in a team game, teammates get auto-assigned an answer at the end of the round, so we don't want the team to get fake bad times
                        playerAnswers.put(answer.gamePlayerId(), answer.answer());
                    });
        });
        api.on(QuizFatalError.class, quizFatalError -> finishGame());
        api.on(QuizOver.class, quizOver -> finishGame());
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
                PlayerContestantEntity contestantEntity = contestantService.getOrCreateContestant(player);
                assert contestantEntity != null;
                GameContestantEntity gameContestantEntity = gameService.createGameContestant(currentGame, contestantEntity);
                databaseContestants.put(contestant.getGamePlayerId(), gameContestantEntity);
            });
        }else{
            Map<Integer, List<TeamPlayer>> teams = new HashMap<>();
            info.players()
                    .stream()
                    .map(quizPlayer -> new TeamPlayer(quizPlayer.gamePlayerId(), quizPlayer.playerName(), Optional.ofNullable(quizPlayer.teamNumber()).orElseThrow()))
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
                GameContestantEntity gameTeamEntity = gameService.createGameContestant(currentGame, contestantService.getOrCreateContestant(team));
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
        lobbyStateManager.getGameMode().score(gameContestant, new AnswerResult(answerResult.score(), answerResult.correct(), playerAnswerTime));
    }

    private void recordAnswerResultsPerTeam(List<PlayerAnswerResult> answerResults, GameSongEntity gameSong){
        Map<Integer, Integer> gamePlayerIdToTeamNumber = new HashMap<>();
        players.values().stream()
                .filter(TeamPlayer.class::isInstance)
                .map(TeamPlayer.class::cast)
                .forEach(teamPlayer -> gamePlayerIdToTeamNumber.put(teamPlayer.getGamePlayerId(), teamPlayer.getTeamNumber()));
        Map<Integer, String> teamNumberToAnswers = new HashMap<>(); //everyone on a team has the same answer
        Map<Integer, Boolean> teamNumberToCorrect = new HashMap<>();//and same result
        Map<Integer, Integer> teamNumberToScore = new HashMap<>();//and same score
        answerResults.forEach(answerResult -> {
            if(!gamePlayerIdToTeamNumber.containsKey(answerResult.gamePlayerId())){
                log.error("gamePlayerId not in gamePlayerIdToTeamNumber {}, {}", answerResult.gamePlayerId(), answerResult);
                return;
            }
            int teamNumber = gamePlayerIdToTeamNumber.get(answerResult.gamePlayerId());
            String playerAnswer = playerAnswers.get(answerResult.gamePlayerId());
            if(playerAnswer != null && !playerAnswer.isBlank()){
                teamNumberToAnswers.put(teamNumber, playerAnswer);
            }
            teamNumberToCorrect.put(teamNumber, answerResult.correct());
            teamNumberToScore.put(teamNumber, answerResult.score());
        });
        Map<Integer, Duration> teamNumberToCalculatedAnswerTime = calculateAnswerTimeForTeams(answerResults, gamePlayerIdToTeamNumber);

        //to protect against nulls we'll use teamNumberToCorrect as the source of ids
        teamNumberToCorrect.keySet().stream()
                .map(contestants::get)
                .filter(ContestantTeam.class::isInstance)
                .map(ContestantTeam.class::cast)
                .forEach(contestantTeam -> {
            int teamNumber = contestantTeam.getTeamNumber();
            GameContestantEntity gameContestantEntity = databaseContestants.get(teamNumber);
            assert gameContestantEntity != null;
            String answer = teamNumberToAnswers.get(teamNumber);
            boolean correct = teamNumberToCorrect.get(teamNumber);
            int score = teamNumberToScore.get(teamNumber);
            Duration answerTime = teamNumberToCalculatedAnswerTime.get(teamNumber);
            gameService.createGameAnswer(gameSong, gameContestantEntity.getContestant(), correct, answer, answerTime);
            lobbyStateManager.getGameMode().score(contestantTeam, new AnswerResult(score, correct, answerTime));
        });
    }

    private Map<Integer, Duration> calculateAnswerTimeForTeams(List<PlayerAnswerResult> answerResults, Map<Integer, Integer> gamePlayerIdToTeamNumber){
        Map<Integer, List<Duration>> teamNumberToAnswerTimes = new HashMap<>();
        answerResults.forEach(answerResult -> {
            if(!gamePlayerIdToTeamNumber.containsKey(answerResult.gamePlayerId())){
                log.error("gamePlayerId not in gamePlayerIdToTeamNumber {}, {}", answerResult.gamePlayerId(), answerResult);
                return;
            }
            int teamNumber = gamePlayerIdToTeamNumber.get(answerResult.gamePlayerId());
            teamNumberToAnswerTimes.putIfAbsent(teamNumber, new ArrayList<>());
            Duration playerAnswerTime = playerAnswerTimes.get(answerResult.gamePlayerId());
            if(playerAnswerTime != null){
                teamNumberToAnswerTimes.get(teamNumber).add(playerAnswerTime);
            }
        });
        Map<Integer, Duration> teamNumberToCalculatedAnswerTime = new HashMap<>();
        teamNumberToAnswerTimes.forEach((teamNumber, answerTimes) -> {
            if(answerTimes.size() == 2){
                //as a special case, if there are only two players on a team, the answer time is the first answer time
                //because they'll either be right together or have a tie and be wrong together
                answerTimes.stream()
                        .min(Duration::compareTo)
                        .ifPresent(min -> teamNumberToCalculatedAnswerTime.put(teamNumber, min));
            }
            answerTimes.stream()
                    .max(Duration::compareTo)
                    .ifPresent(max -> teamNumberToCalculatedAnswerTime.put(teamNumber, max));
        });
        return teamNumberToCalculatedAnswerTime;
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
