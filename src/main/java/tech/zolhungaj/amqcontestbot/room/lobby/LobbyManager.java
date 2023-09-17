package tech.zolhungaj.amqcontestbot.room.lobby;


import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tech.zolhungaj.amqapi.clientcommands.lobby.StartGame;
import tech.zolhungaj.amqapi.servercommands.gameroom.game.QuizOver;
import tech.zolhungaj.amqapi.servercommands.gameroom.game.QuizReady;
import tech.zolhungaj.amqcontestbot.ApiManager;
import tech.zolhungaj.amqcontestbot.commands.ChatCommands;
import tech.zolhungaj.amqcontestbot.chat.ChatController;
import tech.zolhungaj.amqcontestbot.commands.Grant;
import tech.zolhungaj.amqcontestbot.commands.VoteManager;
import tech.zolhungaj.amqcontestbot.database.enums.RulesetEnum;
import tech.zolhungaj.amqcontestbot.database.enums.ScoringTypeEnum;
import tech.zolhungaj.amqcontestbot.exceptions.IncorrectArgumentCountException;
import tech.zolhungaj.amqcontestbot.exceptions.IncorrectCommandUsageException;
import tech.zolhungaj.amqcontestbot.gamemode.GameMode;
import tech.zolhungaj.amqcontestbot.gamemode.GameModeFactory;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class LobbyManager {
    private static final int WAIT_TIME = 30;
    private static final int SECONDARY_WAIT_TIME = 10;
    private static final int MAX_WAIT_TIME = WAIT_TIME + SECONDARY_WAIT_TIME;

    private final LobbyStateManager stateManager;
    private final ApiManager api;
    private final ChatController chatController;
    private final ChatCommands chatCommands;
    private final VoteManager voteManager;

    @PostConstruct
    private void init(){
        chatCommands.register((sender, unused) -> {
            counter = WAIT_TIME;
            startIfPossible();
        }, Grant.OWNER, "start");
        chatCommands.register((sender, unused) -> {
            counter = MAX_WAIT_TIME;
            startIfPossible();
        }, Grant.OWNER, "startnow");
        chatCommands.register((sender, arguments) -> {
            if(!stateManager.isInLobby()){
                throw new IncorrectCommandUsageException("gamemode.vote.not-in-lobby");
            }
            if(arguments.size() < 2 || arguments.size() > 3){
                throw new IncorrectArgumentCountException(2, 3);
            }
            String ruleset = arguments.get(0);
            RulesetEnum rulesetEnum = RulesetEnum.fromName(ruleset);
            if(rulesetEnum == null){
                throw new IncorrectCommandUsageException("gamemode.vote.invalid-ruleset", ruleset);
            }
            String scoringMode = arguments.get(1);
            ScoringTypeEnum scoringTypeEnum = ScoringTypeEnum.fromName(scoringMode);
            if(scoringTypeEnum == null){
                throw new IncorrectCommandUsageException("gamemode.vote.invalid-scoring-mode", scoringMode);
            }
            final GameMode gameMode;
            if(arguments.size() < 3){
                gameMode = GameModeFactory.getGameMode(rulesetEnum, scoringTypeEnum);
            }else{
                int teamSize = Integer.parseInt(arguments.get(2));
                gameMode = GameModeFactory.getGameMode(rulesetEnum, scoringTypeEnum, teamSize);
            }
            if(gameMode.sameGameMode(stateManager.getGameMode())){
                throw new IncorrectCommandUsageException("gamemode.vote.already-current-gamemode", ruleset);
            }
            voteManager.startCommandVote(stateManager.getPlayerNames(), () -> {
                if(stateManager.isInLobby() && !inStartPhase){
                    stateManager.setGameMode(gameMode);
                    counter = 0;
                    changeGameModeCounter = 0;
                    stateManager.cycleGameMode();
                }
                chatController.send("gamemode.vote.success", ruleset, scoringMode);
            }, sender);
        }, "gamemode");
    }
    private boolean inStartPhase = false;
    private int counter = 0;
    private int changeGameModeCounter = 0;

    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.SECONDS)
    public void updateState(){
        if(!stateManager.isInLobby() || inStartPhase){
            counter = 0;
            changeGameModeCounter = 0;
            return;
        }
        changeGameModeCounter++;
        if(stateManager.getPlayerCount() == 0){
            counter = 0;
            stateManager.resetConsecutiveGames();
            if(changeGameModeCounter >= WAIT_TIME){
                changeGameModeCounter = 0;
                stateManager.cycleGameMode();
            }
            return;
        }
        if(stateManager.noFullFileServersOnline()){
            return;
        }
        if(!startIfPossible()){
            sendCountdownBasedMessage();
            counter++;
        }
    }

    private boolean startIfPossible(){
        if(stateManager.noFullFileServersOnline()){
            chatController.send("lobby.starting.failed.no-full-file-servers");
            return false;
        }
        Set<LobbyPlayer> notReady = stateManager.notReadyPlayers();
        if(counter >= WAIT_TIME && notReady.isEmpty()){
            this.start();
            return true;
        }
        if(counter >= MAX_WAIT_TIME){
            this.start();
            return true;
        }
        return false;
    }

    private void sendCountdownBasedMessage(){
        if(counter < WAIT_TIME){
            int diff = WAIT_TIME - counter;
            if(diff % 10 == 0){
                chatController.send("lobby.countdown.major", diff);
            }
            if(diff <= 5){
                chatController.send("lobby.countdown.final", diff);
            }
        }else if(counter == WAIT_TIME){
            Set<LobbyPlayer> notReady = stateManager.notReadyPlayers();
            String message = notReady.stream()
                    .map(LobbyPlayer::playerName)
                    .map("@"::concat) //prefix each name with @ to trigger mention
                    .collect(Collectors.joining(" "));
            chatController.send("lobby.get-ready", message, SECONDARY_WAIT_TIME);
        }else{
            int diff = MAX_WAIT_TIME - counter;
            chatController.send("lobby.countdown.secondary", diff);
        }
    }

    private void start(){
        counter = 0;
        chatController.send("lobby.starting");
        stateManager.incrementConsecutiveGames();
        api.sendCommand(new StartGame());
        inStartPhase = true;
        handleFailedGameStart();
    }

    private void handleFailedGameStart(){
        //in the event that we try to start the game, but no players are ready
        //then the server will ignore our request completely, and we will never get a QuizReady event
        CompletableFuture<Boolean> readyFuture = new CompletableFuture<>();
        api.once(QuizReady.class, quizReady -> {
            log.debug("Quiz ready, {}", quizReady);
            readyFuture.complete(true);
            return true;
        });
        api.once(QuizOver.class, quizOver -> {
            //in the event that the game fails to start, but with a normal error we immediately get the quizOver event
            if(!readyFuture.isDone()){ //guard since this event is also triggered when the game ends normally
                log.debug("Quiz over early, {}", quizOver);
                chatController.send("lobby.starting.failed.generic");
                readyFuture.complete(true); //true because the timeout did not trigger
                counter = WAIT_TIME - 5; //shorten the countdown to make misses less boring
            }
            return true;
        });
        readyFuture.completeOnTimeout(false, 10, TimeUnit.SECONDS);
        readyFuture.thenAccept(ready -> {
            if(Boolean.FALSE.equals(ready)){
                chatController.send("lobby.starting.failed.timeout");
                stateManager.onStartOfLobbyPhase();
            }
            inStartPhase = false;
        });
    }
}
