package tech.zolhungaj.amqcontestbot.room.lobby;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;
import tech.zolhungaj.amqapi.clientcommands.lobby.ChangeRoomSettings;
import tech.zolhungaj.amqapi.clientcommands.lobby.MovePlayerToSpectator;
import tech.zolhungaj.amqapi.clientcommands.roombrowser.HostMultiplayerRoom;
import tech.zolhungaj.amqapi.servercommands.gameroom.SpectatorLeft;
import tech.zolhungaj.amqapi.servercommands.gameroom.game.GameStarting;
import tech.zolhungaj.amqapi.servercommands.gameroom.game.QuizOver;
import tech.zolhungaj.amqapi.servercommands.gameroom.lobby.NewPlayer;
import tech.zolhungaj.amqapi.servercommands.gameroom.PlayerLeft;
import tech.zolhungaj.amqapi.servercommands.gameroom.lobby.GameHosted;
import tech.zolhungaj.amqapi.servercommands.gameroom.lobby.PlayerChangedToSpectator;
import tech.zolhungaj.amqapi.servercommands.gameroom.lobby.PlayerReadyChange;
import tech.zolhungaj.amqapi.servercommands.gameroom.lobby.SpectatorChangedToPlayer;
import tech.zolhungaj.amqapi.servercommands.globalstate.FileServerStatus;
import tech.zolhungaj.amqapi.servercommands.globalstate.LoginComplete;
import tech.zolhungaj.amqapi.sharedobjects.gamesettings.GameSettings;
import tech.zolhungaj.amqcontestbot.ApiManager;
import tech.zolhungaj.amqcontestbot.chat.ChatController;
import tech.zolhungaj.amqcontestbot.database.enums.RulesetEnum;
import tech.zolhungaj.amqcontestbot.database.enums.ScoringTypeEnum;
import tech.zolhungaj.amqcontestbot.gamemode.GameMode;
import tech.zolhungaj.amqcontestbot.gamemode.GameModeFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class LobbyStateManager {
    private static final List<String> FULL_FILE_SERVERS = List.of("catboxEu", "catboxNaOne", "catboxNaTwo");
    private final ApiManager api;
    private final ChatController chatController;
    @Setter
    @Getter
    private GameMode gameMode = GameModeFactory.getGameMode(RulesetEnum.MASTER_OF_SEASONS, ScoringTypeEnum.COUNT);
    @Getter
    private boolean inLobby = false;
    private final Map<Integer, LobbyPlayer> players = new ConcurrentHashMap<>();
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

        //queue related events
        api.on(SpectatorLeft.class, this::removeSpectator); //a leaving spectator leaves the queue
        //TODO: queue join and leave, in someone decides to camp with ready off
    }

    public List<String> getPlayerNames(){
        return players.values().stream()
                .filter(LobbyPlayer::inLobby)
                .map(LobbyPlayer::playerName)
                .toList();
    }

    private void loginComplete(LoginComplete loginComplete){
        GameSettings initialSettings = gameMode.getNextSettings();
        api.sendCommand(new HostMultiplayerRoom(initialSettings));
        loginComplete.serverStatuses().forEach(this::updateFileServerStatus);
    }

    private void updateFileServerStatus(FileServerStatus fileServerStatus){
        fileServerState.put(fileServerStatus.serverName(), fileServerStatus.online());
        sendMessageAboutFileServers();
    }

    private void sendMessageAboutFileServers(){
        if(noFullFileServersOnline()){
            chatController.send("lobby.full-file-servers-online.none");
        }else{
            chatController.send("lobby.full-file-servers-online.some");
        }
    }

    private void gameHosted(GameHosted gameHosted){
        this.players.clear();
        this.queue.clear();
        gameHosted.players().forEach(this::newPlayer);
        inLobby = true;
        moveToSpectator(api.getSelfName());
    }

    private void gameStarting(GameStarting gameStarting){
        this.inLobby = false;
    }

    private void quizOver(QuizOver quizOver){
        this.queue.clear();
        this.queue.addAll(quizOver.playersInQueue());
        this.players.replaceAll((id, player) -> player.withInLobby(false));
        quizOver.players().forEach(this::newPlayer);
        onStartOfLobbyPhase();
    }

    public void onStartOfLobbyPhase(){
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

    private void removeSpectator(SpectatorLeft spectatorLeft){
        removeSpectator(spectatorLeft.playerName());
    }
    private void removeSpectator(String playerName){
        removeFromQueue(playerName);
    }

    private void addToQueue(String playerName){
        queue.add(playerName);
    }

    private void removeFromQueue(String playerName){
        queue.removeIf(playerName::equals);
    }

    public boolean noFullFileServersOnline(){
        return fileServerState.entrySet().stream()
                .filter(entry -> FULL_FILE_SERVERS.contains(entry.getKey()))
                .noneMatch(Map.Entry::getValue);
    }

    public long getPlayerCount(){
        return players.values().stream()
                .filter(LobbyPlayer::inLobby)
                .count();
    }

    public Set<LobbyPlayer> notReadyPlayers(){
        return players.values().stream()
                .filter(LobbyPlayer::inLobby)
                .filter(Predicate.not(LobbyPlayer::isReady))
                .collect(Collectors.toSet());
    }

    public void resetConsecutiveGames(){
        players.replaceAll((id, player) -> player.withConsecutiveGames(0));
    }

    public void incrementConsecutiveGames(){
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

    public void cycleGameMode(){
        GameSettings nextSettings = gameMode.getNextSettings();
        api.sendCommand(new ChangeRoomSettings(nextSettings));
    }

    private void leaveRoom(){
        players.clear();
        queue.clear();
        inLobby = false;
    }
}
