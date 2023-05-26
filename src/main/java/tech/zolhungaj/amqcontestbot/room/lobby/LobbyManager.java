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
import tech.zolhungaj.amqapi.servercommands.gameroom.NewPlayer;
import tech.zolhungaj.amqapi.servercommands.gameroom.PlayerLeft;
import tech.zolhungaj.amqapi.servercommands.gameroom.SpectatorJoined;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
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
    private String selfName = "";
    private boolean inLobby = false;
    private int gameId = -1;
    private final Map<Integer, LobbyPlayer> players = new ConcurrentHashMap<>();
    private final Set<String> spectators = new HashSet<>();
    private final Queue<String> queue = new ConcurrentLinkedQueue<>();
    private final Map<String, Boolean> fileServerState = new HashMap<>();

    @PostConstruct
    public void init(){
        api.on(LoginComplete.class, loginComplete -> {
            selfName = loginComplete.selfName();
            currentSettings = gameMode.getNextSettings();
            api.sendCommand(new HostRoom(currentSettings));
            loginComplete.serverStatuses().forEach(serverStatus -> fileServerState.put(serverStatus.serverName(), serverStatus.online()));
        });
        api.on(tech.zolhungaj.amqapi.servercommands.gameroom.lobby.HostGame.class, hostGame -> {
            this.players.clear();
            this.spectators.clear();
            this.queue.clear();
            hostGame.players().stream()
                    .map(this::newPlayerToLobbyPlayer)
                    .forEach(lobbyPlayer -> this.players.put(lobbyPlayer.gamePlayerId(), lobbyPlayer));
            inLobby = true;
            gameId = hostGame.gameId();
            moveToSpectator(selfName);
        });
        api.on(NewPlayer.class, newPlayer -> addPlayer(newPlayerToLobbyPlayer(newPlayer)));
        api.on(SpectatorChangedToPlayer.class, spectatorChangedToPlayer -> {
            addPlayer(playerToLobbyPlayer(spectatorChangedToPlayer));
            removeSpectator(spectatorChangedToPlayer.playerName());
        });
        api.on(PlayerLeft.class, playerLeft -> removePlayer(playerLeft.player().gamePlayerId().orElse(-1)));
        api.on(SpectatorJoined.class, spectatorJoined -> addSpectator(spectatorJoined.playerName()));
        api.on(PlayerChangedToSpectator.class, playerChangedToSpectator -> {
            removePlayer(playerChangedToSpectator.playerDescription().gamePlayerId().orElse(-1));
            addSpectator(playerChangedToSpectator.spectatorDescription().playerName());
        });
        api.on(PlayerReadyChange.class, playerReadyChange -> this.players.computeIfPresent(
                playerReadyChange.gamePlayerId(),
                (key, player) -> player.withReady(playerReadyChange.ready()))
        );
        api.on(FileServerStatus.class, fileServerStatus -> {
            fileServerState.put(fileServerStatus.serverName(), fileServerStatus.online());
            sendMessageAboutFileServers();
        });
        //TODO: queue
        //TODO: game end or failed to open
        //TODO: leave room
    }

    private void addPlayer(LobbyPlayer lobbyPlayer){
        //get consecutiveGames in case somebody leaves and rejoins to reset their value
        int consecutiveGames = this.players.getOrDefault(lobbyPlayer.gamePlayerId(), lobbyPlayer).consecutiveGames();
        this.players.put(lobbyPlayer.gamePlayerId(), lobbyPlayer.withConsecutiveGames(consecutiveGames));
        log.info("{}", players);
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

    private void addSpectator(String playerName){
        spectators.add(playerName);
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

    private void onStartOfLobbyPhase(){
        emptyQueueIfPossible();
        inLobby = true;
        cycleGameMode();
    }

    //TODO: hookup, should happen after players and queue has been updated after game
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
