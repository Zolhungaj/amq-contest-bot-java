package tech.zolhungaj.amqcontestbot.chat;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tech.zolhungaj.amqapi.servercommands.gameroom.GameChatMessage;
import tech.zolhungaj.amqapi.servercommands.gameroom.GameChatUpdate;
import tech.zolhungaj.amqcontestbot.ApiManager;
import tech.zolhungaj.amqcontestbot.moderation.NameResolutionFailedException;
import tech.zolhungaj.amqcontestbot.moderation.NameResolver;

import java.time.Instant;
import java.util.function.Predicate;

@Component
@RequiredArgsConstructor
public class UtilityCommands {

    private final ApiManager api;
    private final NameResolver nameResolver;
    private final ChatCommands chatCommands;
    private final ChatController chatController;

    @PostConstruct
    private void init(){
        registerPing();
        registerSay();
        registerResolve();
    }

    private void registerPing(){
        chatCommands.register((sender, arguments) -> {
            long ping = api.getPing();
            String sentMessage = chatController.send("ping.base", ping).get(0);
            Instant sendInstant = Instant.now();
            long sendInstantAsMillis = sendInstant.toEpochMilli();
            Predicate<GameChatMessage> printWhenMatch = gameChatMessage -> {
                if(gameChatMessage.sender().equals(api.getSelfName())
                        && gameChatMessage.message().equals(sentMessage)){
                    Instant receiveInstant = Instant.now();
                    long receiveInstantAsMillis = receiveInstant.toEpochMilli();
                    long difference = receiveInstantAsMillis - sendInstantAsMillis;
                    chatController.send("ping.chat", difference);
                    return true;
                }
                return false;
            };
            //will probably build up once-functions for one of these, but unlikely to be a problem
            api.once(GameChatMessage.class, printWhenMatch);
            api.once(GameChatUpdate.class, gameChatUpdate -> gameChatUpdate.messages().stream().anyMatch(printWhenMatch));
        }, "ping");
    }

    private void registerSay(){
        chatCommands.register(
                (sender, arguments) -> chatController.sendRaw(String.join(" ", arguments)),
                ChatCommands.Grant.ADMIN,
                "say"
        );
    }

    private void registerResolve(){
        chatCommands.register((sender, arguments) -> {
            if(arguments.size() != 1){
                throw new IllegalArgumentException();
            }
            String nickname = arguments.get(0);
            try{
                String originalName = nameResolver.resolveOriginalName(nickname);
                chatController.send("name-resolver.result", nickname, originalName);
            }catch(NameResolutionFailedException e){
                chatController.send("name-resolver.not-found", nickname);
            }
        }, ChatCommands.Grant.NONE, "resolve");
    }
}
