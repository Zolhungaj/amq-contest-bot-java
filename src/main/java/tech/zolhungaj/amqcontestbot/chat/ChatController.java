package tech.zolhungaj.amqcontestbot.chat;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tech.zolhungaj.amqapi.clientcommands.ClientCommandType;
import tech.zolhungaj.amqapi.clientcommands.EmptyClientCommand;
import tech.zolhungaj.amqcontestbot.ApiManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;

@Slf4j
@Component
public class ChatController {
    private static final int MESSAGE_LIMIT = 150;
    private final ApiManager api;
    private final ConcurrentLinkedQueue<String> pendingMessages = new ConcurrentLinkedQueue<>();
    public ChatController(@Autowired ApiManager api){
        this.api = api;
    }

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
        final List<String> messageChunked;
        if(message.length() <= MESSAGE_LIMIT){
            messageChunked = List.of(message);
        }else{
            //split into words that can fit inside the message limit
            final String splitter = " ";
            List<String> messageSplit = Stream
                    .of(message.split(splitter))
                    .flatMap(word -> {
                        List<String> chunks = new ArrayList<>();
                        while(word.length() > MESSAGE_LIMIT){
                            String left = word.substring(0,MESSAGE_LIMIT);
                            chunks.add(left);
                            word = word.substring(MESSAGE_LIMIT);
                        }
                        chunks.add(word);
                        return chunks.stream();
                    })
                    .toList();
            List<String> chunks = new ArrayList<>();
            final StringBuilder builder = new StringBuilder();
            for(String word : messageSplit){
                if(builder.length() + splitter.length() + word.length() <= MESSAGE_LIMIT){
                    builder.append(splitter);
                    builder.append(word);
                }else{
                    chunks.add(builder.toString());
                    builder.delete(0, MESSAGE_LIMIT);
                }
            }
            if(!builder.isEmpty()){
                chunks.add(builder.toString());
            }
            messageChunked = List.copyOf(chunks);
        }
        log.info("Sending chat '{}'", message);
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
            //TODO: once send message is implemented in API
            api.sendCommand(new EmptyClientCommand(ClientCommandType.START_TRACKING_ONLINE_USERS) {
            });
        }
    }
}
