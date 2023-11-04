package tech.zolhungaj.amqcontestbot.commands;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tech.zolhungaj.amqapi.clientcommands.quiz.ContinueQuiz;
import tech.zolhungaj.amqapi.clientcommands.quiz.PauseQuiz;
import tech.zolhungaj.amqapi.clientcommands.quiz.StartReturnToLobbyVote;
import tech.zolhungaj.amqapi.servercommands.gameroom.GameChatMessage;
import tech.zolhungaj.amqapi.servercommands.gameroom.GameChatUpdate;
import tech.zolhungaj.amqcontestbot.ApiManager;
import tech.zolhungaj.amqcontestbot.chat.ChatController;
import tech.zolhungaj.amqcontestbot.chat.DirectMessageController;
import tech.zolhungaj.amqcontestbot.database.enums.RulesetEnum;
import tech.zolhungaj.amqcontestbot.database.enums.ScoringTypeEnum;
import tech.zolhungaj.amqcontestbot.exceptions.IncorrectArgumentCountException;
import tech.zolhungaj.amqcontestbot.exceptions.NameResolutionFailedException;
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
    private final DirectMessageCommands dmCommands;
    private final DirectMessageController dmController;

    @PostConstruct
    private void init(){
        registerPing();
        registerSay();
        registerResolve();
        registerLobby();
        registerGamemodesHelp();
        registerPause();
    }

    private void registerPing(){
        chatCommands.register((sender, arguments) -> {
            String sentMessage = chatController.send("ping.base").get(0);
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
                Grant.OWNER,
                "say"
        );
        dmCommands.register(
                (sender, arguments) -> chatController.sendRaw(String.join(" ", arguments)),
                Grant.OWNER,
                "say"
        );
        dmCommands.register(
                (sender, arguments) -> dmController.sendRaw(sender, String.join(" ", arguments)),
                Grant.OWNER,
                "reply"
        );
    }

    private void registerResolve(){
        chatCommands.register((sender, arguments) -> {
            if(arguments.size() != 1){
                throw new IncorrectArgumentCountException(1);
            }
            String nickname = arguments.get(0);
            try{
                String originalName = nameResolver.resolveOriginalName(nickname);
                chatController.send("name-resolver.result", nickname, originalName);
            }catch(NameResolutionFailedException e){
                chatController.send("name-resolver.not-found", nickname);
            }
        }, Grant.NONE, "resolve");
    }

    private void registerLobby() {
        chatCommands.register(
                (sender, arguments) -> api.sendCommand(new StartReturnToLobbyVote()),
                Grant.NONE,
                "lobby",
                "returntolobby",
                "quit"
        );
    }

    private void registerGamemodesHelp(){
        final String rulesetsCommand = "rulesets";
        final String scoreTypesCommand = "scoretypes";
        chatCommands.register(
                (sender, arguments) -> chatController.send("commands.gamemodes", rulesetsCommand, scoreTypesCommand),
                Grant.NONE,
                "gamemodes"
        );
        chatCommands.register(
                (sender, arguments) -> {
                    chatController.send("commands." + rulesetsCommand);
                    RulesetEnum.allNames().forEach(rulesetNames -> chatController.sendRaw(String.join(", ", rulesetNames)));
                },
                Grant.NONE,
                rulesetsCommand
        );
        chatCommands.register(
                (sender, arguments) -> {
                    chatController.send("commands." + scoreTypesCommand);
                    ScoringTypeEnum.allNames().forEach(chatController::sendRaw);
                },
                Grant.NONE,
                scoreTypesCommand
        );
    }
  
    private void registerPause() {
        chatCommands.register(
                (sender, arguments) -> api.sendCommand(new PauseQuiz()),
                Grant.NONE,
                "pause"
        );
        chatCommands.register(
                (sender, arguments) -> api.sendCommand(new ContinueQuiz()),
                Grant.NONE,
                "continue", "unpause"
        );
    }
}
