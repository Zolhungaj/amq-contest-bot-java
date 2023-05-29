package tech.zolhungaj.amqcontestbot.room.lobby;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tech.zolhungaj.amqapi.clientcommands.lobby.ChangeRoomSettings;
import tech.zolhungaj.amqapi.clientcommands.lobby.MovePlayerToSpectator;
import tech.zolhungaj.amqapi.clientcommands.lobby.StartGame;
import tech.zolhungaj.amqapi.clientcommands.roombrowser.HostRoom;
import tech.zolhungaj.amqapi.servercommands.gameroom.SpectatorLeft;
import tech.zolhungaj.amqapi.servercommands.gameroom.game.GameStarting;
import tech.zolhungaj.amqapi.servercommands.gameroom.game.QuizOver;
import tech.zolhungaj.amqapi.servercommands.gameroom.game.QuizReady;
import tech.zolhungaj.amqapi.servercommands.gameroom.lobby.NewPlayer;
import tech.zolhungaj.amqapi.servercommands.gameroom.PlayerLeft;
import tech.zolhungaj.amqapi.servercommands.gameroom.SpectatorJoined;
import tech.zolhungaj.amqapi.servercommands.gameroom.lobby.GameHosted;
import tech.zolhungaj.amqapi.servercommands.gameroom.lobby.PlayerChangedToSpectator;
import tech.zolhungaj.amqapi.servercommands.gameroom.lobby.PlayerReadyChange;
import tech.zolhungaj.amqapi.servercommands.gameroom.lobby.SpectatorChangedToPlayer;
import tech.zolhungaj.amqapi.servercommands.globalstate.FileServerStatus;
import tech.zolhungaj.amqapi.servercommands.globalstate.LoginComplete;
import tech.zolhungaj.amqapi.sharedobjects.gamesettings.GameSettings;
import tech.zolhungaj.amqcontestbot.ApiManager;
import tech.zolhungaj.amqcontestbot.chat.ChatController;
import tech.zolhungaj.amqcontestbot.gamemode.GameMode;
import tech.zolhungaj.amqcontestbot.gamemode.MasterOfTheSeasonsGameMode;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class LobbyManager {
    private static final int WAIT_TIME = 30;
    private static final int SECONDARY_WAIT_TIME = 10;
    private static final List<String> FULL_FILE_SERVERS = List.of("catbox");
    private final ApiManager api;
    private final ChatController chatController;
    private final GameMode gameMode = new MasterOfTheSeasonsGameMode();
    private GameSettings currentSettings = null;
    private int counter = 0;
    private boolean inLobby = false;
    private int gameId = -1;
    private final Map<Integer, LobbyPlayer> players = new ConcurrentHashMap<>();
    private final Set<String> spectators = new HashSet<>();
    private final Queue<String> queue = new ConcurrentLinkedQueue<>();
    private final Map<String, Boolean> fileServerState = new HashMap<>();

    @PostConstruct
    private void init(){
        //global state
        api.on(LoginComplete.class, this::loginComplete);
        api.on(FileServerStatus.class, this::updateFileServerStatus);

        //state of room
        api.on(GameHosted.class, this::gameHosted);
        api.on(GameStarting.class, this::gameStarting);
        api.on(QuizOver.class, this::quizOver);
        //TODO: leave room (voluntarily or forced)

        //status on present players
        api.on(NewPlayer.class, this::newPlayer);
        api.on(SpectatorChangedToPlayer.class, this::spectatorChangedToPlayer);
        api.on(PlayerReadyChange.class, this::playerReadyChange);

        //players leaving
        api.on(PlayerLeft.class, this::playerLeft);
        api.on(PlayerChangedToSpectator.class, this::playerChangedToSpectator);

        //spectators, is this needed?
        api.on(SpectatorJoined.class, this::spectatorJoined);
        api.on(SpectatorLeft.class, this::removeSpectator);

        //TODO: queue for the failed to host state
    }

    private void loginComplete(LoginComplete loginComplete){
        currentSettings = gameMode.getNextSettings();
        api.sendCommand(new HostRoom(currentSettings));
        loginComplete.serverStatuses().forEach(this::updateFileServerStatus);
    }

    private void updateFileServerStatus(FileServerStatus fileServerStatus){
        fileServerState.put(fileServerStatus.serverName(), fileServerStatus.online());
        sendMessageAboutFileServers();
    }

    private void gameHosted(GameHosted gameHosted){
        this.players.clear();
        this.spectators.clear();
        this.queue.clear();
        gameHosted.players().stream()
                .map(this::newPlayerToLobbyPlayer)
                .forEach(lobbyPlayer -> this.players.put(lobbyPlayer.gamePlayerId(), lobbyPlayer));
        inLobby = true;
        gameId = gameHosted.gameId();
        moveToSpectator(api.getSelfName());
    }

    private void gameStarting(GameStarting gameStarting){
        this.inLobby = false;
    }

    private void quizOver(QuizOver quizOver){
        this.queue.clear();
        this.queue.addAll(quizOver.playersInQueue());
        this.spectators.clear();
        List<String> newSpectators = quizOver.spectators().stream().map(SpectatorJoined::playerName).toList();
        this.spectators.addAll(newSpectators);
        this.players.replaceAll((id, player) -> player.withInLobby(false));
        quizOver.players().forEach(this::newPlayer);
        onStartOfLobbyPhase();
    }

    private void onStartOfLobbyPhase(){
        emptyQueueIfPossible();
        inLobby = true;
        cycleGameMode();
    }

    private void newPlayer(NewPlayer player){
        addPlayer(newPlayerToLobbyPlayer(player));
    }

    private void spectatorChangedToPlayer(SpectatorChangedToPlayer spectatorChangedToPlayer){
        addPlayer(playerToLobbyPlayer(spectatorChangedToPlayer));
        removeSpectator(spectatorChangedToPlayer.playerName());
    }

    private void addPlayer(LobbyPlayer lobbyPlayer){
        //get consecutiveGames in case somebody leaves and rejoins to reset their value
        int consecutiveGames = this.players.getOrDefault(lobbyPlayer.gamePlayerId(), lobbyPlayer).consecutiveGames();
        this.players.put(lobbyPlayer.gamePlayerId(), lobbyPlayer.withConsecutiveGames(consecutiveGames));
        log.info("{}", players);
    }

    private void playerReadyChange(PlayerReadyChange playerReadyChange){
        this.players.computeIfPresent(
                playerReadyChange.gamePlayerId(),
                (key, player) -> player.withReady(playerReadyChange.ready())
        );
    }

    private void playerLeft(PlayerLeft playerLeft){
        removePlayer(playerLeft.player().gamePlayerId().orElse(-1));
    }

    private void playerChangedToSpectator(PlayerChangedToSpectator playerChangedToSpectator){
        removePlayer(playerChangedToSpectator.playerDescription().gamePlayerId().orElse(-1));
        addSpectator(playerChangedToSpectator.spectatorDescription().playerName());
    }

    private void removePlayer(int gamePlayerId){
        players.computeIfPresent(gamePlayerId, (key, player) -> player.withInLobby(false));
        log.info("{}", players);
    }

    private LobbyPlayer newPlayerToLobbyPlayer(NewPlayer player){
        return new LobbyPlayer(
                player.playerName(),
                player.gamePlayerId(),
                player.ready(),
                true,
                player.teamNumber(),
                0);
    }

    private LobbyPlayer playerToLobbyPlayer(SpectatorChangedToPlayer player){
        return new LobbyPlayer(
                player.playerName(),
                player.gamePlayerId(),
                player.ready(),
                true,
                player.teamNumber(),
                0);
    }

    private void spectatorJoined(SpectatorJoined spectatorJoined){
        addSpectator(spectatorJoined.playerName());
    }
    private void addSpectator(String playerName){
        spectators.add(playerName);
    }

    private void removeSpectator(SpectatorLeft spectatorLeft){
        spectators.remove(spectatorLeft.playerName());
    }
    private void removeSpectator(String playerName){
        spectators.remove(playerName);
    }

    private void sendMessageAboutFileServers(){
        if(noFullFileServersOnline()){
            chatController.send("lobby.full-file-servers-online.none");
        }else{
            chatController.send("lobby.full-file-servers-online.some");
        }
    }

    private void addToQueue(String playerName){
        queue.add(playerName);
    }

    private void removeFromQueue(String playerName){
        queue.removeIf(playerName::equals);
    }

    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.SECONDS)
    public void updateState(){
        if(!inLobby){
            counter = 0;
            return;
        }
        if(playerCount() == 0){
            counter = 0;
            resetConsecutiveGames();
            return;
        }
        if(noFullFileServersOnline()){
            return;
        }
        if(!startIfPossible()){
            sendCountdownBasedMessage();
            counter++;
        }
    }

    private boolean noFullFileServersOnline(){
        return fileServerState.entrySet().stream()
                .filter(entry -> FULL_FILE_SERVERS.contains(entry.getKey()))
                .noneMatch(Map.Entry::getValue);
    }

    private long playerCount(){
        return players.values().stream()
                .filter(LobbyPlayer::inLobby)
                .count();
    }

    private Set<LobbyPlayer> notReadyPlayers(){
        return players.values().stream()
                .filter(LobbyPlayer::inLobby)
                .filter(Predicate.not(LobbyPlayer::isReady))
                .collect(Collectors.toSet());
    }

    private boolean startIfPossible(){
        Set<LobbyPlayer> notReady = notReadyPlayers();
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

    private void start(){
        counter = 0;
        chatController.send("lobby.starting");
        incrementConsecutiveGames();
        api.sendCommand(new StartGame());
        inLobby = false;
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
                onStartOfLobbyPhase();
            }
        });
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
            Set<LobbyPlayer> notReady = notReadyPlayers();
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

    private void resetConsecutiveGames(){
        players.replaceAll((id, player) -> player.withConsecutiveGames(0));
    }

    private void incrementConsecutiveGames(){
        players.replaceAll((id, player) -> {
            if(player.inLobby()){
                return player.withConsecutiveGames(player.consecutiveGames() + 1);
            }else{
                return player.withConsecutiveGames(0);
            }
        });
    }

    private void emptyQueueIfPossible(){
        final List<LobbyPlayer> playersEligibleForChanging = new ArrayList<>(this.players.values().stream()
                .filter(LobbyPlayer::inLobby)
                .filter(player -> player.consecutiveGames() > 0) //don't move newly joined players
                .toList());
        Collections.shuffle(playersEligibleForChanging);//shuffle to make fair in ties
        playersEligibleForChanging.stream()
                .sorted(Comparator.comparingInt(LobbyPlayer::consecutiveGames)
                        .reversed()) //those present for longer get moved first
                .limit(queue.size())
                .map(LobbyPlayer::playerName)
                .forEach(this::moveToSpectator);
    }

    private void moveToSpectator(String playerName){
        api.sendCommand(new MovePlayerToSpectator(playerName));
    }

    private void cycleGameMode(){
        GameSettings nextSettings = gameMode.getNextSettings();
        api.sendCommand(new ChangeRoomSettings(nextSettings));
    }

    private void leaveRoom(){
        gameId = -1;
        players.clear();
        spectators.clear();
        queue.clear();
        inLobby = false;
        counter = 0;
    }
}
