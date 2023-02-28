package tech.zolhungaj.amqcontestbot;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Util {
    private static final Pattern GUEST_MATCHER = Pattern.compile("^Guest-\\d{5}$");
    private Util(){}

    public static boolean isGuest(String playerName){
        return GUEST_MATCHER.matcher(playerName).matches();
    }

    public static List<String> chunkMessageToFitLimits(String message, int messageLimit){
        if(message.length() <= messageLimit){
            return List.of(message);
        }else{
            //split into words that can fit inside the message limit
            final String splitter = " ";
            List<String> messageSplit = Stream
                    .of(message.split(splitter))
                    .flatMap(chunkWordToLimitsFunction(messageLimit))
                    .toList();
            List<String> chunks = new ArrayList<>();
            final StringBuilder builder = new StringBuilder();
            for(String word : messageSplit){
                if(builder.length() + splitter.length() + word.length() <= messageLimit){
                    builder.append(splitter);
                    builder.append(word);
                }else{
                    chunks.add(builder.toString());
                    builder.delete(0, messageLimit);
                }
            }
            if(!builder.isEmpty()){
                chunks.add(builder.toString());
            }
            return List.copyOf(chunks);
        }
    }

    private static Function<String, Stream<String>> chunkWordToLimitsFunction(int messageLimit){
        return word -> chunkWordToLimits(word, messageLimit);
    }

    private static Stream<String> chunkWordToLimits(String word, int messageLimit){
        Stream.Builder<String> chunks = Stream.builder();
        while(word.length() > messageLimit){
            String left = word.substring(0,messageLimit);
            chunks.add(left);
            word = word.substring(messageLimit);
        }
        chunks.add(word);
        return chunks.build();
    }
}
