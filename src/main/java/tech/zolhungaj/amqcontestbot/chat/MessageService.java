package tech.zolhungaj.amqcontestbot.chat;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class MessageService {

    public String getMessage(String i18nCanonicalName, List<Object> arguments){
        log.debug("Start replacement {}", i18nCanonicalName);
        String message = getMessageForCanonicalName(i18nCanonicalName);
        for(var i = 0; i < arguments.size(); i++){
            String search = "%" + (i+1) + "%";
            message = message.replaceAll(search, String.valueOf(arguments.get(i)));
        }
        return message;
    }
    private String getMessageForCanonicalName(String i18nCanonicalName){
        return i18nCanonicalName + "%1% %2% %3% %4% %5% %6% %7%";//TODO: implement once database is done
    }

    public String censor(String message){
        return message.replace("ROBOT", "MACHINE");//TODO:implement censoring preventing certain phrases from being spoken, once database is done
    }
}
