package tech.zolhungaj.amqcontestbot.moderation;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tech.zolhungaj.amqapi.clientcommands.lobby.Kick;
import tech.zolhungaj.amqapi.servercommands.gameroom.NewPlayer;
import tech.zolhungaj.amqapi.servercommands.gameroom.SpectatorJoined;
import tech.zolhungaj.amqcontestbot.ApiManager;
import tech.zolhungaj.amqcontestbot.chat.ChatCommands;
import tech.zolhungaj.amqcontestbot.repository.PlayerService;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class PunishmentManager {
    private final ApiManager api;
    private final NameResolver nameResolver;
    private final PlayerService playerService;
    private final ChatCommands chatCommands;

    private final Set<String> kickedThisSession = Collections.synchronizedSet(new HashSet<>());

    @PostConstruct
    public void init(){
        api.on(command -> {
            if(command instanceof SpectatorJoined spectatorJoined){
                handleJoin(spectatorJoined.playerName());
            }
            if(command instanceof NewPlayer newPlayer){
                handleJoin(newPlayer.playerName());
            }
            return true;
        });
        registerBanCommands();
        registerBanTrueCommands();
        registerKickCommands();
    }

    private void registerBanCommands(){
        chatCommands.register((sender, arguments) -> {
            if(arguments.size() != 1){
                throw new IllegalArgumentException();
            }
            String nickname = arguments.get(0);
            ban(nickname);
        }, ChatCommands.Grant.MODERATOR, "ban");
        chatCommands.register((sender, arguments) -> {
            if(arguments.size() != 1){
                throw new IllegalArgumentException();
            }
            String nickname = arguments.get(0);
            unban(nickname);
        }, ChatCommands.Grant.MODERATOR, "unban");
    }

    private void registerBanTrueCommands(){
        chatCommands.register((sender, arguments) -> {
            if(arguments.size() != 1){
                throw new IllegalArgumentException();
            }
            String trueName = arguments.get(0);
            banByTrueName(trueName);
        }, ChatCommands.Grant.MODERATOR, "bantrue");
        chatCommands.register((sender, arguments) -> {
            if(arguments.size() != 1){
                throw new IllegalArgumentException();
            }
            String trueName = arguments.get(0);
            unbanByTrueName(trueName);
        }, ChatCommands.Grant.MODERATOR,"unbantrue");
    }

    private void registerKickCommands(){
        chatCommands.register((sender, arguments) -> {
            if(arguments.size() != 1){
                throw new IllegalArgumentException();
            }
            String nickname = arguments.get(0);
            kick(nickname);
        }, ChatCommands.Grant.MODERATOR, "kick", "yeet");
        chatCommands.register((sender, arguments) -> {
            if(arguments.size() != 1){
                throw new IllegalArgumentException();
            }
            String nickname = arguments.get(0);
            unkick(nickname);
        }, ChatCommands.Grant.MODERATOR, "unkick", "unyeet");
    }

    private void ban(String nickname){
        kick(nickname);
        nameResolver.getTrueName(nickname)
                .thenAccept(this::banByTrueName);
    }
    private void banByTrueName(String trueName){
        if(!playerService.isModerator(trueName)){
            playerService.ban(trueName);
        }
    }
    private void unban(String nickname){
        unkick(nickname);
        nameResolver.getTrueName(nickname)
                .thenAccept(this::unbanByTrueName);
    }

    private void unbanByTrueName(String trueName){
        playerService.unban(trueName);
    }

    public void kick(String nickname){
        nameResolver.getTrueName(nickname)
                .thenAccept(trueName -> {
                    if(!playerService.isModerator(trueName)){
                        kickedThisSession.add(trueName);
                        kickInternal(nickname);
                    }
                });
    }

    private void unkick(String nickname){
        nameResolver.getTrueName(nickname).thenAccept(kickedThisSession::remove);
    }

    private void handleJoin(String nickname){
        nameResolver.getTrueName(nickname).thenAccept(trueName -> {
            if(kickedThisSession.contains(trueName) || playerService.isBanned(trueName)){
                kickInternal(nickname);
            }
        });
    }

    private void kickInternal(String nickname){
        api.sendCommand(new Kick(nickname));
    }
}
