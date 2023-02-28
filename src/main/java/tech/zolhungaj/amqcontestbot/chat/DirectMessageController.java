package tech.zolhungaj.amqcontestbot.chat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tech.zolhungaj.amqcontestbot.ApiManager;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import static tech.zolhungaj.amqcontestbot.Util.chunkMessageToFitLimits;

@Slf4j
@Component
@RequiredArgsConstructor
public class DirectMessageController {
    public static final int MESSAGE_LIMIT = 200;
    private final ApiManager api;
    private final MessageService messageService;
    private final ConcurrentLinkedQueue<DM> pendingMessages = new ConcurrentLinkedQueue<>();

    public List<DM> send(String recipient, String i18nCanonicalName, Object... arguments){
        return send(recipient, i18nCanonicalName, List.of(arguments));
    }
    public List<DM> send(String recipient, String i18nCanonicalName, List<Object> arguments){
        String message = messageService.getMessage(i18nCanonicalName, arguments);
        return sendRaw(recipient, message);
    }
    public List<DM> sendRaw(String recipient, String message){
        String censoredMessage = messageService.censor(message);
        final List<String> messageChunked = chunkMessageToFitLimits(censoredMessage, MESSAGE_LIMIT);
        final List<DM> dmList = messageChunked.stream().map(msg -> new DM(recipient, msg)).toList();
        log.info("Queueing DM '{}' to {}", censoredMessage, recipient);
        pendingMessages.addAll(dmList);
        return dmList;
    }

    @Scheduled(fixedDelay = 1_000_000 / 15, timeUnit = TimeUnit.MICROSECONDS)
    private void sendMessage(){
        DM nextMessage = pendingMessages.poll();
        if(nextMessage != null){
            //TODO: send once implemented in API
        }
    }

    public record DM(String recipient, String message){}
}
