package tech.zolhungaj.amqcontestbot.moderation;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tech.zolhungaj.amqapi.clientcommands.lobby.Kick;
import tech.zolhungaj.amqapi.servercommands.gameroom.lobby.NewPlayer;
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
    private void init(){
        api.on(SpectatorJoined.class, spectatorJoined -> handleJoin(spectatorJoined.playerName()));
        api.on(NewPlayer.class, newPlayer -> handleJoin(newPlayer.playerName()));
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
            String originalName = arguments.get(0);
            banByOriginalName(originalName);
        }, ChatCommands.Grant.MODERATOR, "banbyoriginalname", "bantrue");
        chatCommands.register((sender, arguments) -> {
            if(arguments.size() != 1){
                throw new IllegalArgumentException();
            }
            String originalName = arguments.get(0);
            unbanByOriginalName(originalName);
        }, ChatCommands.Grant.MODERATOR, "unbanbyoriginalname", "unbantrue");
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
        nameResolver.resolveOriginalNameAsync(nickname)
                .thenAccept(this::banByOriginalName);
    }
    private void banByOriginalName(String originalName){
        if(!playerService.isModerator(originalName)){
            playerService.ban(originalName);
        }
    }
    private void unban(String nickname){
        unkick(nickname);
        nameResolver.resolveOriginalNameAsync(nickname)
                .thenAccept(this::unbanByOriginalName);
    }

    private void unbanByOriginalName(String originalName){
        playerService.unban(originalName);
    }

    public void kick(String nickname){
        nameResolver.resolveOriginalNameAsync(nickname)
                .thenAccept(originalName -> {
                    if(!playerService.isModerator(originalName)){
                        kickedThisSession.add(originalName);
                        kickInternal(nickname);
                    }
                });
    }

    private void unkick(String nickname){
        nameResolver.resolveOriginalNameAsync(nickname)
                .thenAccept(kickedThisSession::remove);
    }

    private void handleJoin(String nickname){
        nameResolver.resolveOriginalNameAsync(nickname).thenAccept(originalName -> {
            if(kickedThisSession.contains(originalName) || playerService.isBanned(originalName)){
                kickInternal(nickname);
            }
        });
    }

    private void kickInternal(String nickname){
        api.sendCommand(new Kick(nickname));
    }
}
