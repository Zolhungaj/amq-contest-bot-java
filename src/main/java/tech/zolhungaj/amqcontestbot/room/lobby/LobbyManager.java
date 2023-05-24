package tech.zolhungaj.amqcontestbot.room.lobby;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tech.zolhungaj.amqapi.clientcommands.lobby.MovePlayerToSpectator;
import tech.zolhungaj.amqapi.clientcommands.lobby.StartGame;
import tech.zolhungaj.amqapi.clientcommands.roombrowser.HostRoom;
import tech.zolhungaj.amqapi.servercommands.gameroom.NewPlayer;
import tech.zolhungaj.amqapi.servercommands.gameroom.PlayerLeft;
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
        api.on(command -> {
            if(command instanceof LoginComplete loginComplete){
                selfName = loginComplete.selfName();
                currentSettings = gameMode.getNextSettings();
                api.sendCommand(new HostRoom(currentSettings));
                loginComplete.serverStatuses().forEach(serverStatus -> fileServerState.put(serverStatus.serverName(), serverStatus.online()));
            }else if(command instanceof tech.zolhungaj.amqapi.servercommands.gameroom.lobby.HostGame hostGame){
                this.players.clear();
                this.spectators.clear();
                this.queue.clear();
                hostGame.players().stream()
                        .map(this::newPlayerToLobbyPlayer)
                        .forEach(lobbyPlayer -> this.players.put(lobbyPlayer.gamePlayerId(), lobbyPlayer));
                inLobby = true;
                gameId = hostGame.gameId();
                moveToSpectator(selfName);
            }else if(command instanceof NewPlayer newPlayer){
                addPlayer(newPlayerToLobbyPlayer(newPlayer));
            }else if(command instanceof SpectatorChangedToPlayer spectatorChangedToPlayer){
                addPlayer(playerToLobbyPlayer(spectatorChangedToPlayer));
                removeSpectator(spectatorChangedToPlayer.playerName());
            }else if(command instanceof PlayerLeft playerLeft){
                int gamePlayerId = playerLeft.player().gamePlayerId().orElse(-1);
                removePlayer(gamePlayerId);
            }else if(command instanceof PlayerChangedToSpectator toSpectator){
                int gamePlayerId = toSpectator.playerDescription().gamePlayerId().orElse(-1);
                removePlayer(gamePlayerId);
                addSpectator(toSpectator.spectatorDescription().playerName());
            }else if(command instanceof PlayerReadyChange playerReadyChange){
                this.players.computeIfPresent(playerReadyChange.gamePlayerId(), (key, player) -> player.withReady(playerReadyChange.ready()));
            }else if(command instanceof FileServerStatus fileServerStatus){
                fileServerState.put(fileServerStatus.serverName(), fileServerStatus.online());
            }
            //TODO: queue
            //TODO: game end or failed to open
            //TODO: leave room
            return true;
        });
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

    private void leaveRoom(){
        gameId = -1;
        players.clear();
        spectators.clear();
        queue.clear();
        inLobby = false;
        counter = 0;
    }
}
