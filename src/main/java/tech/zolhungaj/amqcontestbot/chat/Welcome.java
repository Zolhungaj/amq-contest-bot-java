package tech.zolhungaj.amqcontestbot.chat;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tech.zolhungaj.amqapi.servercommands.gameroom.lobby.NewPlayer;
import tech.zolhungaj.amqapi.servercommands.gameroom.SpectatorJoined;
import tech.zolhungaj.amqapi.servercommands.gameroom.lobby.SpectatorChangedToPlayer;
import tech.zolhungaj.amqapi.servercommands.objects.PlayerAvatar;
import tech.zolhungaj.amqcontestbot.ApiManager;
import tech.zolhungaj.amqcontestbot.database.model.PlayerEntity;
import tech.zolhungaj.amqcontestbot.moderation.NameResolver;
import tech.zolhungaj.amqcontestbot.database.service.PlayerService;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class Welcome {

    private final ChatController chatController;
    private final QuipGenerator quipGenerator;
    private final NameResolver nameResolver;
    private final PlayerService playerService;
    private final ApiManager api;

    @PostConstruct
    private void init(){
        api.on(SpectatorJoined.class, spectatorJoined -> newSpectator(spectatorJoined.playerName()));
        api.on(NewPlayer.class, player -> newPlayer(player.playerName(), player.level(), player.avatar()));
        api.on(SpectatorChangedToPlayer.class, spectatorChangedToPlayer ->
                spectatorChangedToPlayer(
                        spectatorChangedToPlayer.playerName(),
                        spectatorChangedToPlayer.level(),
                        spectatorChangedToPlayer.avatar()
                )
        );
    }

    private void newPlayer(String nickname, int level, PlayerAvatar avatar){
        nameResolver
                .resolveOriginalNameAsync(nickname)
                .thenAccept(originalName -> newPlayer(nickname, originalName, level, avatar));
    }

    private void newPlayer(String nickname, String originalName, int currentLevel, PlayerAvatar avatar){
        Optional<PlayerEntity> player = playerService.getPlayer(originalName);
        final PlayerEntity playerEntity;
        if(player.isEmpty()){
            chatController.send("welcome.new.player", nickname);
            playerEntity = new PlayerEntity();
        }else{
            playerEntity = player.get();
            quipGenerator.commentOnPlayer(playerEntity, nickname, currentLevel, avatar, false);
        }
        playerEntity.setOriginalName(originalName);
        playerEntity.setLevel(currentLevel);
        playerEntity.setAvatar(avatar);
        playerService.save(playerEntity);
    }

    private void spectatorChangedToPlayer(String nickname, int level, PlayerAvatar avatar){
        nameResolver
                .resolveOriginalNameAsync(nickname)
                .thenAccept(originalName -> spectatorChangedToPlayer(nickname, originalName, level, avatar));
    }

    private void spectatorChangedToPlayer(String nickname, String originalName, int currentLevel, PlayerAvatar avatar){
        Optional<PlayerEntity> player = playerService.getPlayer(originalName);
        if(player.isPresent()){
            PlayerEntity playerEntity = player.get();
            quipGenerator.commentOnPlayer(playerEntity, nickname, currentLevel, avatar, true);
            playerEntity.setLevel(currentLevel);
            playerEntity.setAvatar(avatar);
            playerService.save(playerEntity);
        }
    }

    private void newSpectator(String nickname){
        nameResolver
                .resolveOriginalNameAsync(nickname)
                .thenAccept(originalName -> newSpectator(nickname, originalName));
    }
    private void newSpectator(String nickname, String originalName){
        Optional<PlayerEntity> player = playerService.getPlayer(originalName);
        if(player.isEmpty()){
            chatController.send("welcome.new.spectator", nickname);
            PlayerEntity playerEntity = new PlayerEntity();
            playerEntity.setOriginalName(originalName);
            playerService.save(playerEntity);
        }else{
            chatController.send("welcome.returning.spectator", nickname);
        }
    }
}
