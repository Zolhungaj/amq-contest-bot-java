package tech.zolhungaj.amqcontestbot.chat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tech.zolhungaj.amqapi.clientcommands.lobby.SendPublicChatMessage;
import tech.zolhungaj.amqcontestbot.ApiManager;
import tech.zolhungaj.amqcontestbot.database.service.InternationalisationService;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import static tech.zolhungaj.amqcontestbot.Util.chunkMessageToFitLimits;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatController {
    public static final int MESSAGE_LIMIT = 150;
    private final ApiManager api;
    private final InternationalisationService internationalisationService;
    private final ConcurrentLinkedQueue<String> pendingMessages = new ConcurrentLinkedQueue<>();
    private String currentLanguage = "en"; //TODO hook up to commands
    private String currentSubLanguage = "gb";

    public List<String> send(String i18nCanonicalName, Object... arguments){
        return send(i18nCanonicalName, List.of(arguments));
    }
    public List<String> send(String i18nCanonicalName, List<Object> arguments){
        String message = internationalisationService.getMessage(i18nCanonicalName, currentLanguage, currentSubLanguage, arguments);
        return sendRaw(message);
    }
    public List<String> sendRaw(String message){
        String censoredMessage = internationalisationService.censor(message);
        final List<String> messageChunked = chunkMessageToFitLimits(censoredMessage, MESSAGE_LIMIT);
        log.info("Sending chat '{}'", censoredMessage);
        pendingMessages.addAll(messageChunked);
        return messageChunked;
    }

    @Scheduled(fixedDelay = 500)
    private void sendMessage(){
        String nextMessage = pendingMessages.poll();
        if(nextMessage != null){
            api.sendCommand(new SendPublicChatMessage(nextMessage));
        }
    }
}
