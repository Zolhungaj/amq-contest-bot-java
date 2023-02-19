package tech.zolhungaj.amqcontestbot.moderation;

import org.springframework.beans.factory.annotation.Autowired;
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
public class PunishmentManager {
    private final ApiManager api;
    private final NameResolver nameResolver;
    private final PlayerService playerService;

    private final Set<String> kickedThisSession = Collections.synchronizedSet(new HashSet<>());

    public PunishmentManager(@Autowired ApiManager api,
                             @Autowired NameResolver nameResolver,
                             @Autowired PlayerService playerService,
                             @Autowired ChatCommands chatCommands){
        this.api = api;
        this.nameResolver = nameResolver;
        this.playerService = playerService;
        this.api.on(command -> {
            if(command instanceof SpectatorJoined spectatorJoined){
                handleJoin(spectatorJoined.playerName());
            }
            if(command instanceof NewPlayer newPlayer){
                handleJoin(newPlayer.playerName());
            }
            return true;
        });
        this.registerBanCommands(chatCommands);
        this.registerBanTrueCommands(chatCommands);
        this.registerKickCommands(chatCommands);
    }

    private void registerBanCommands(ChatCommands chatCommands){
        chatCommands.register((sender, arguments) -> {
            if(arguments.size() != 1){
                throw new IllegalArgumentException();
            }
            String nickname = arguments.get(0);
            if(playerService.isModerator(sender)){
                ban(nickname);
            }
        }, "ban");
        chatCommands.register((sender, arguments) -> {
            if(arguments.size() != 1){
                throw new IllegalArgumentException();
            }
            String nickname = arguments.get(0);
            if(playerService.isModerator(sender)){
                unban(nickname);
            }
        }, "unban");
    }

    private void registerBanTrueCommands(ChatCommands chatCommands){
        chatCommands.register((sender, arguments) -> {
            if(arguments.size() != 1){
                throw new IllegalArgumentException();
            }
            String trueName = arguments.get(0);
            if(playerService.isModerator(sender)){
                banByTrueName(trueName);
            }
        }, "bantrue");
        chatCommands.register((sender, arguments) -> {
            if(arguments.size() != 1){
                throw new IllegalArgumentException();
            }
            String trueName = arguments.get(0);
            if(playerService.isModerator(sender)){
                unbanByTrueName(trueName);
            }
        }, "unbantrue");
    }

    private void registerKickCommands(ChatCommands chatCommands){
        chatCommands.register((sender, arguments) -> {
            if(arguments.size() != 1){
                throw new IllegalArgumentException();
            }
            String nickname = arguments.get(0);
            if(playerService.isModerator(sender)){
                kick(nickname);
            }
        }, "kick", "yeet");
        chatCommands.register((sender, arguments) -> {
            if(arguments.size() != 1){
                throw new IllegalArgumentException();
            }
            String nickname = arguments.get(0);
            if(playerService.isModerator(sender)){
                unkick(nickname);
            }
        }, "unkick", "unyeet");
    }

    private void ban(String nickname){
        kick(nickname);
        nameResolver.getTrueName(nickname).thenAccept(this::banByTrueName);
    }
    private void banByTrueName(String trueName){
        if(!playerService.isModerator(trueName)){
            playerService.ban(trueName);
        }
    }
    private void unban(String nickname){
        String trueName = nameResolver.getTrueNameBlocking(nickname);
        unbanByTrueName(trueName);
        unkick(nickname);
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
        this.api.sendCommand(new Kick(nickname));
    }
}
