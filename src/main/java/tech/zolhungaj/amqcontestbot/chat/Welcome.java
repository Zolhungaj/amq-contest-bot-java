package tech.zolhungaj.amqcontestbot.chat;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tech.zolhungaj.amqapi.servercommands.gameroom.lobby.NewPlayer;
import tech.zolhungaj.amqapi.servercommands.gameroom.SpectatorJoined;
import tech.zolhungaj.amqapi.servercommands.objects.PlayerAvatar;
import tech.zolhungaj.amqcontestbot.ApiManager;
import tech.zolhungaj.amqcontestbot.moderation.NameResolver;
import tech.zolhungaj.amqcontestbot.repository.PlayerService;

@Component
@RequiredArgsConstructor
public class Welcome {

    private final ChatController chatController;
    private final NameResolver nameResolver;
    private final PlayerService playerService;
    private final ApiManager api;

    @PostConstruct
    private void init(){
        api.on(SpectatorJoined.class, spectatorJoined -> newSpectator(spectatorJoined.playerName()));
        api.on(NewPlayer.class, player -> newPlayer(player.playerName(), player.level(), player.avatar()));
    }

    private void newPlayer(String nickname, int level, PlayerAvatar avatar){
        nameResolver
                .resolveOriginalNameAsync(nickname)
                .thenAccept(originalName -> newPlayer(nickname, originalName, level, avatar));
    }

    private void newPlayer(String nickname, String originalName, int level, PlayerAvatar avatar){

    }

    private void newSpectator(String nickname){
        nameResolver
                .resolveOriginalNameAsync(nickname)
                .thenAccept(originalName -> newSpectator(nickname, originalName));
    }
    private void newSpectator(String nickname, String originalName){
        Object player = playerService.getPlayer(originalName);
        //TODO: handle the rest
    }
}
