package tech.zolhungaj.amqcontestbot.chat;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tech.zolhungaj.amqapi.clientcommands.lobby.SendMessage;
import tech.zolhungaj.amqcontestbot.ApiManager;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import static tech.zolhungaj.amqcontestbot.Util.chunkMessageToFitLimits;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatController {
    public static final int MESSAGE_LIMIT = 150;
    private final ApiManager api;
    private final ChatCommands chatCommands;
    private final MessageService messageService;
    private final ConcurrentLinkedQueue<String> pendingMessages = new ConcurrentLinkedQueue<>();

    @PostConstruct
    public void init(){
        chatCommands.register(
                (sender, arguments) -> sendRaw(String.join(" ", arguments)),
                ChatCommands.Grant.ADMIN,
                "say"
        );
    }

    public List<String> send(String i18nCanonicalName, Object... arguments){
        return send(i18nCanonicalName, List.of(arguments));
    }
    public List<String> send(String i18nCanonicalName, List<Object> arguments){
        String message = messageService.getMessage(i18nCanonicalName, arguments);
        return sendRaw(message);
    }
    public List<String> sendRaw(String message){
        String censoredMessage = messageService.censor(message);
        final List<String> messageChunked = chunkMessageToFitLimits(censoredMessage, MESSAGE_LIMIT);
        log.info("Sending chat '{}'", censoredMessage);
        pendingMessages.addAll(messageChunked);
        return messageChunked;
    }

    @Scheduled(fixedDelay = 500)
    private void sendMessage(){
        String nextMessage = pendingMessages.poll();
        if(nextMessage != null){
            api.sendCommand(SendMessage.builder().message(nextMessage).build());
        }
    }
}
