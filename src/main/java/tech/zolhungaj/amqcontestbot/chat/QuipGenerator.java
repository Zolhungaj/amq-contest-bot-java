package tech.zolhungaj.amqcontestbot.chat;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.random.RandomGenerator;

/** More of a for-fun module, occasionally comments on songs and player achievements.
 * This is also responsible for introductions to the bot for new players
 */
@Component
@RequiredArgsConstructor
public class QuipGenerator {
    private double chattiness = 0.25;
    private final RandomGenerator randomGenerator = RandomGenerator.of("L64X128MixRandom");
    private final ChatController chatController;
    private final ChatCommands chatCommands;

    @PostConstruct
    private void init(){
        chatCommands.register((sender, arguments) -> {
            final double value;
            if(arguments.size() == 1){
                value = Double.parseDouble(arguments.get(0));
            }else{
                throw new IllegalArgumentException();
            }
            chattiness = value;
        }, ChatCommands.Grant.MODERATOR, "setchattiness");
    }

    private void quipPlaceholder(){
        if(randomGenerator.nextDouble() >= (1.0 - chattiness)){
            chatController.send("quip");
        }
    }
}
