package tech.zolhungaj.amqcontestbot.chat;

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
    private final ConcurrentLinkedQueue<String> pendingMessages = new ConcurrentLinkedQueue<>();

    public List<String> send(String i18nCanonicalName, Object... arguments){
        return this.send(i18nCanonicalName, List.of(arguments));
    }
    public List<String> send(String i18nCanonicalName, List<Object> arguments){
        log.debug("Start replacement {}", i18nCanonicalName);
        String message = getMessageForCanonicalName(i18nCanonicalName);
        for(var i = 0; i < arguments.size(); i++){
            String search = "%" + (i+1) + "%";
            message = message.replaceAll(search, arguments.get(i).toString());
        }
        return sendRaw(message);
    }
    public List<String> sendRaw(String message){
        String censoredMessage = selfCensor(message);
        final List<String> messageChunked = chunkMessageToFitLimits(censoredMessage, MESSAGE_LIMIT);
        log.info("Sending chat '{}'", censoredMessage);
        pendingMessages.addAll(messageChunked);
        return messageChunked;
    }

    private String getMessageForCanonicalName(String i18nCanonicalName){
        return i18nCanonicalName + "%1% %2% %3% %4% %5% %6% %7%";//TODO: implement once database is done
    }

    @Scheduled(fixedDelay = 500)
    private void sendMessage(){
        String nextMessage = pendingMessages.poll();
        if(nextMessage != null){
            api.sendCommand(SendMessage.builder().message(nextMessage).build());
        }
    }

    private String selfCensor(String message){
        return message.replace("ROBOT", "MACHINE");//TODO:implement censoring preventing certain phrases from being spoken
    }
}
