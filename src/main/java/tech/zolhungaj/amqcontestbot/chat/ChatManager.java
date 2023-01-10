package tech.zolhungaj.amqcontestbot.chat;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tech.zolhungaj.amqcontestbot.ApiManager;

import java.util.List;

@Slf4j
@Component
public class ChatManager {
    private static final int MESSAGE_LIMIT = 150;
    private final ApiManager api;
    public ChatManager(@Autowired ApiManager api){
        this.api = api;
    }

    public void send(String i18nCanonicalName, Object... arguments){
        this.send(i18nCanonicalName, List.of(arguments));
    }
    public void send(String i18nCanonicalName, List<Object> arguments){
        log.debug("Start replacement {}", i18nCanonicalName);
        String message = getMessageForCanonicalName(i18nCanonicalName);
        for(var i = 0; i < arguments.size(); i++){
            String search = "%" + (i+1) + "%";
            message = message.replaceAll(search, arguments.get(i).toString());
        }
        sendRaw(message);
    }
    public void sendRaw(String message){
        log.info("Sending chat '{}'", message);
        //TODO: once send message is implemented in API
        //api.sendCommand();
    }

    private String getMessageForCanonicalName(String i18nCanonicalName){
        return i18nCanonicalName + "%1% %2% %3% %4% %5% %6% %7%";//TODO: implement once database is done
    }

}
