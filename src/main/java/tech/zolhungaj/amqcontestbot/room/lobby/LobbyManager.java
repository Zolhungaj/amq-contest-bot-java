package tech.zolhungaj.amqcontestbot.room.lobby;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tech.zolhungaj.amqapi.clientcommands.lobby.StartGame;
import tech.zolhungaj.amqapi.servercommands.gameroom.game.QuizOver;
import tech.zolhungaj.amqapi.servercommands.gameroom.game.QuizReady;
import tech.zolhungaj.amqcontestbot.ApiManager;
import tech.zolhungaj.amqcontestbot.chat.ChatController;

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

    private final LobbyStateManager stateManager;
    private final ApiManager api;
    private final ChatController chatController;
    private boolean inStartPhase = false;
    private int counter = 0;

    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.SECONDS)
    public void updateState(){
        if(!stateManager.isInLobby() || inStartPhase){
            counter = 0;
            return;
        }
        if(stateManager.getPlayerCount() == 0){
            counter = 0;
            stateManager.resetConsecutiveGames();
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
        Set<LobbyPlayer> notReady = stateManager.notReadyPlayers();
        if(counter >= WAIT_TIME && notReady.isEmpty()){
            this.start();
            return true;
        }
        if(counter >= WAIT_TIME + SECONDARY_WAIT_TIME){
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
            int diff = WAIT_TIME + SECONDARY_WAIT_TIME - counter;
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
                readyFuture.complete(false);
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
