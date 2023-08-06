package tech.zolhungaj.amqcontestbot.moderation;

import jakarta.annotation.PostConstruct;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tech.zolhungaj.amqapi.clientcommands.lobby.Kick;
import tech.zolhungaj.amqapi.servercommands.gameroom.lobby.NewPlayer;
import tech.zolhungaj.amqapi.servercommands.gameroom.SpectatorJoined;
import tech.zolhungaj.amqcontestbot.ApiManager;
import tech.zolhungaj.amqcontestbot.chat.ChatCommands;
import tech.zolhungaj.amqcontestbot.chat.DirectMessageController;
import tech.zolhungaj.amqcontestbot.database.service.ModerationService;

import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class PunishmentManager {
    private final ApiManager api;
    private final NameResolver nameResolver;
    private final ModerationService moderationService;
    private final ChatCommands chatCommands;
    private final DirectMessageController directMessageController;

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
            if(arguments.size() < 2){
                throw new IllegalArgumentException();
            }
            String nickname = arguments.get(0);
            String duration = arguments.get(1);
            final String reason;
            if(arguments.size() >= 3){
                reason = String.join(" ", arguments.subList(2, arguments.size()));
            }else{
                reason = "No reason given";
            }
            ban(nickname, sender, duration, reason);
        }, ChatCommands.Grant.ADMIN, "ban");
        chatCommands.register((sender, arguments) -> {
            if(arguments.size() != 1){
                throw new IllegalArgumentException();
            }
            String nickname = arguments.get(0);
            unban(nickname, sender);
        }, ChatCommands.Grant.ADMIN, "unban");
    }

    private void registerBanTrueCommands(){
        chatCommands.register((sender, arguments) -> {
            if(arguments.size() < 2){
                throw new IllegalArgumentException();
            }
            String originalName = arguments.get(0);
            String duration = arguments.get(1);
            final String reason;
            if(arguments.size() >= 3){
                reason = String.join(" ", arguments.subList(2, arguments.size()));
            }else{
                reason = "No reason given";
            }
            banByOriginalName(originalName, sender, duration, reason);
        }, ChatCommands.Grant.ADMIN, "banbyoriginalname", "bantrue");
        chatCommands.register((sender, arguments) -> {
            if(arguments.size() != 1){
                throw new IllegalArgumentException();
            }
            String originalName = arguments.get(0);
            unbanByOriginalName(originalName, sender);
        }, ChatCommands.Grant.ADMIN, "unbanbyoriginalname", "unbantrue");
    }

    private void registerKickCommands(){
        chatCommands.register((sender, arguments) -> {
            if(arguments.isEmpty()){
                throw new IllegalArgumentException();
            }
            String nickname = arguments.get(0);
            final String reason;
            if(arguments.size() >= 2){
                reason = String.join(" ", arguments.subList(2, arguments.size()));
            }else{
                reason = "No reason given";
            }
            kick(nickname, sender, reason);
        }, ChatCommands.Grant.MODERATOR, "kick", "yeet");
        chatCommands.register((sender, arguments) -> {
            if(arguments.size() != 1){
                throw new IllegalArgumentException();
            }
            String nickname = arguments.get(0);
            unkick(nickname);
        }, ChatCommands.Grant.MODERATOR, "unkick", "unyeet");
    }

    private void ban(String nickname, String sender, String duration, String reason){
        kick(nickname, sender, reason);
        nameResolver.resolveOriginalNameAsync(nickname)
                .thenAccept(originalName -> banByOriginalName(originalName, sender, duration, reason));
    }
    private void banByOriginalName(String originalName, String sender, String duration, String reason){
        if(!moderationService.isModerator(originalName)){
            moderationService.banCommand(originalName, nameResolver.resolveOriginalName(sender), reason, parseDuration(duration));
        }else{
            throw new IllegalArgumentException("Cannot ban a moderator");
        }
    }

    private Duration parseDuration(@NonNull String duration){
        if(duration.equals("forever")){
            return Duration.ofDays((365L * 9999));
        }
        Pattern pattern = Pattern.compile("^(\\d+)([hdmy])$");
        if(!pattern.matcher(duration).matches()){
            throw new IllegalArgumentException("Invalid duration, please match the format \"[0-9]+[hmdy]\" or \"forever\"");
        }
        return Duration.parse(duration);
    }
    private void unban(String nickname, String sender){
        unkick(nickname);
        nameResolver.resolveOriginalNameAsync(nickname)
                .thenAccept(originalName -> unbanByOriginalName(originalName, sender));
    }

    private void unbanByOriginalName(String originalName, String sender){
        moderationService.unbanCommand(originalName, nameResolver.resolveOriginalName(sender));
    }

    public void kick(String nickname, String sender, String reason){
        nameResolver.resolveOriginalNameAsync(nickname)
                .thenAccept(originalName -> kick(nickname, originalName, sender, reason));
    }

    private void kick(String nickname, String originalName, String sender, String reason){
        if(!moderationService.isModerator(originalName)){
            kickedThisSession.add(originalName);
            kickInternal(nickname);
            moderationService.addLog(nameResolver.resolveOriginalName(sender), "Kicked %s(%s) for %s".formatted(nickname,originalName,reason));
            directMessageController.send(nickname, "dm.kick.reason", reason);
        }else{
            throw new IllegalArgumentException("Cannot kick a moderator");
        }
    }

    private void unkick(String nickname){
        nameResolver.resolveOriginalNameAsync(nickname)
                .thenAccept(kickedThisSession::remove);
    }

    private void handleJoin(String nickname){
        nameResolver.resolveOriginalNameAsync(nickname).thenAccept(originalName -> {
            if(kickedThisSession.contains(originalName) || moderationService.isBanned(originalName)){
                kickInternal(nickname);
            }
        });
    }

    private void kickInternal(String nickname){
        api.sendCommand(new Kick(nickname));
    }
}
