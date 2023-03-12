package tech.zolhungaj.amqcontestbot.room;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tech.zolhungaj.amqapi.clientcommands.roombrowser.HostGame;
import tech.zolhungaj.amqapi.servercommands.gameroom.NewPlayer;
import tech.zolhungaj.amqapi.servercommands.gameroom.PlayerLeft;
import tech.zolhungaj.amqapi.servercommands.gameroom.lobby.PlayerChangedToSpectator;
import tech.zolhungaj.amqapi.servercommands.gameroom.lobby.PlayerReadyChange;
import tech.zolhungaj.amqapi.servercommands.gameroom.lobby.SpectatorChangedToPlayer;
import tech.zolhungaj.amqapi.servercommands.globalstate.LoginComplete;
import tech.zolhungaj.amqapi.servercommands.objects.Player;
import tech.zolhungaj.amqapi.sharedobjects.gamesettings.GameSettings;
import tech.zolhungaj.amqcontestbot.ApiManager;
import tech.zolhungaj.amqcontestbot.gamemode.GameMode;
import tech.zolhungaj.amqcontestbot.gamemode.MasterOfTheSeasonsGameMode;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class LobbyManager {
    private final ApiManager api;
    private final GameMode gameMode = new MasterOfTheSeasonsGameMode();
    private GameSettings currentSettings = null;
    private long counter = 0L;
    private String selfName = "";
    private boolean inLobby = false;
    private final Map<Integer, LobbyPlayer> players = new ConcurrentHashMap<>();

    @PostConstruct
    public void init(){
        api.on(command -> {
            if(command instanceof LoginComplete loginComplete){
                selfName = loginComplete.selfName();
                currentSettings = gameMode.getNextSettings();
                api.sendCommand(new HostGame(currentSettings));
            }else if(command instanceof tech.zolhungaj.amqapi.servercommands.gameroom.lobby.HostGame hostGame){
                this.players.clear();
                hostGame.players().stream()
                        .map(this::newPlayerToLobbyPlayer)
                        .forEach(lobbyPlayer -> this.players.put(lobbyPlayer.gamePlayerId(), lobbyPlayer));
                inLobby = true;
                //TODO: move self to spectators //api.sendCommand(new );
            }else if(command instanceof NewPlayer newPlayer){
                addPlayer(newPlayerToLobbyPlayer(newPlayer));
            }else if(command instanceof SpectatorChangedToPlayer spectatorChangedToPlayer){
                addPlayer(playerToLobbyPlayer(spectatorChangedToPlayer));
            }else if(command instanceof PlayerLeft playerLeft){
                int gamePlayerId = playerLeft.player().gamePlayerId().orElse(-1);
                removePlayer(gamePlayerId);
            }else if(command instanceof PlayerChangedToSpectator toSpectator){
                int gamePlayerId = toSpectator.playerDescription().gamePlayerId().orElse(-1);
                removePlayer(gamePlayerId);
            }else if(command instanceof PlayerReadyChange playerReadyChange){
                this.players.computeIfPresent(playerReadyChange.gamePlayerId(), (key, player) -> player.withReady(player.isReady()));
            }
            return true;
        });
    }

    private void addPlayer(LobbyPlayer lobbyPlayer){
        this.players.put(lobbyPlayer.gamePlayerId(), lobbyPlayer);
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
                player.teamNumber());
    }

    private LobbyPlayer playerToLobbyPlayer(Player player){
        return new LobbyPlayer(
                player.getPlayerName(),
                player.getGamePlayerId(),
                player.getReady(),
                true,
                Optional.ofNullable(player.getTeamNumber()));
    }
    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.SECONDS)
    public void updateState(){
        if(!inLobby){
            return;
        }
        counter++;
    }

    private Set<LobbyPlayer> notReadyPlayers(){
        return players.values().stream()
                .filter(LobbyPlayer::inLobby)
                .filter(Predicate.not(LobbyPlayer::isReady))
                .collect(Collectors.toSet());
    }
}
