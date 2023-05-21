package tech.zolhungaj.amqcontestbot.chat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tech.zolhungaj.amqapi.servercommands.gameroom.NewPlayer;
import tech.zolhungaj.amqapi.servercommands.gameroom.SpectatorJoined;
import tech.zolhungaj.amqapi.servercommands.objects.PlayerAvatar;
import tech.zolhungaj.amqcontestbot.ApiManager;
import tech.zolhungaj.amqcontestbot.moderation.NameResolver;
import tech.zolhungaj.amqcontestbot.repository.PlayerService;

@Component
public class Welcome {

    private final ChatController chatController;
    private final NameResolver nameResolver;
    private final PlayerService playerService;
    public Welcome(@Autowired PlayerService playerService,
                         @Autowired ApiManager api,
                         @Autowired NameResolver nameResolver,
                         @Autowired ChatController chatController){
        this.chatController = chatController;
        this.nameResolver = nameResolver;
        this.playerService = playerService;
        api.on(command -> {
            if(command instanceof SpectatorJoined spectatorJoined){
                newSpectator(spectatorJoined.playerName());
            }
            if(command instanceof NewPlayer newPlayer){
                newPlayer(newPlayer.playerName(), newPlayer.level(), newPlayer.avatar());
            }
            return true;
        });
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
