package tech.zolhungaj.amqcontestbot.chat;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tech.zolhungaj.amqapi.clientcommands.social.SendDirectMessage;
import tech.zolhungaj.amqcontestbot.ApiManager;
import tech.zolhungaj.amqcontestbot.database.service.InternationalisationService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import static tech.zolhungaj.amqcontestbot.Util.chunkMessageToFitLimits;

@Slf4j
@Component
@RequiredArgsConstructor
public class DirectMessageController {
    public static final int MESSAGE_LIMIT = 200;
    private final ApiManager api;
    private final ChatController chatController;
    private final FriendModule friendModule;
    private final InternationalisationService internationalisationService;
    private final ConcurrentLinkedQueue<DM> pendingMessages = new ConcurrentLinkedQueue<>();

    private final Map<String, String> userLanguageChoice = new HashMap<>();
    private final Map<String, String> userSubLanguageChoice = new HashMap<>();
    //TODO: add a way to change language and sublanguage, and save it to the database
    @PostConstruct
    private void init(){
        api.on(command -> {
            if(false){//TODO: command instanceof NewChatAlert
                String alert = "";
                String name = "";
                if(alert.equals("Must be Level 20 to Message non Friends")){
                    friendModule.sendFriendRequest(name);
                    chatController.send("dm.error.must-be-friends", name);
                    pendingMessages.removeIf(dm -> dm.recipient.equals(name));
                }
                return true;
            }
            return false;
        });
    }

    public List<DM> send(String recipient, String i18nCanonicalName, Object... arguments){
        return send(recipient, i18nCanonicalName, List.of(arguments));
    }
    public List<DM> send(String recipient, String i18nCanonicalName, List<Object> arguments){
        String language = userLanguageChoice.getOrDefault(recipient, "en");
        String subLanguage = userSubLanguageChoice.getOrDefault(recipient, "gb");
        String message = internationalisationService.getMessage(i18nCanonicalName, language, subLanguage, arguments);
        return sendRaw(recipient, message);
    }
    public List<DM> sendRaw(String recipient, String message){
        String censoredMessage = internationalisationService.censor(message);
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
            api.sendCommand(new SendDirectMessage(nextMessage.recipient, nextMessage.message));
        }
    }

    public record DM(String recipient, String message){}
}
