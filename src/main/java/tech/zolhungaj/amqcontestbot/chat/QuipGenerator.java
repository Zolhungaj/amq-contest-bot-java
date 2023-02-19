package tech.zolhungaj.amqcontestbot.chat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tech.zolhungaj.amqcontestbot.moderation.NameResolver;
import tech.zolhungaj.amqcontestbot.repository.PlayerService;

import java.util.random.RandomGenerator;

/** More of a for-fun module, occasionally comments on songs and player achievements.
 * This is also responsible for introductions to the bot for new players
 */
@Component
public class QuipGenerator {
    private double chattiness = 0.25;
    private final RandomGenerator randomGenerator = RandomGenerator.of("L64X128MixRandom");
    private final ChatController chatController;
    public QuipGenerator(@Autowired ChatCommands chatCommands,
                         @Autowired PlayerService playerService,
                         @Autowired NameResolver nameResolver,
                         @Autowired ChatController chatController){
        this.chatController = chatController;
        chatCommands.register((sender, arguments) -> {
            final double value;
            if(arguments.size() == 1){
                value = Double.parseDouble(arguments.get(0));
            }else{
                throw new IllegalArgumentException();
            }
            if(playerService.isModerator(nameResolver.getTrueNameBlocking(sender))){
                chattiness = value;
            }
        }, "setchattiness");
    }

    private void quipPlaceholder(){
        if(randomGenerator.nextDouble() >= (1.0 - chattiness)){
            chatController.send("quip");
        }
    }
}
