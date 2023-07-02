package tech.zolhungaj.amqcontestbot.chat;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tech.zolhungaj.amqapi.servercommands.objects.PlayerAvatar;
import tech.zolhungaj.amqcontestbot.database.model.PlayerEntity;

import java.util.random.RandomGenerator;

/** More of a for-fun module, occasionally comments on songs and player achievements.
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

    public void commentOnPlayer(PlayerEntity playerEntity, String nickname, int currentLevel, PlayerAvatar avatar, boolean wasInRoom){

        if(commentOnMilestone(playerEntity, nickname, currentLevel, wasInRoom)){
            return;
        }
        //TODO: comment on avatar
        if(wasInRoom){
            if(randomGenerator.nextDouble()/16 >= (1.0 - chattiness)){
                chatController.send("welcome.spectator-to-player", nickname);
            }
        }else{
            chatController.send("welcome.returning.player", nickname);
        }
    }

    private boolean commentOnMilestone(PlayerEntity playerEntity, String nickname, int currentLevel, boolean wasInRoom){
        if(playerEntity.getLevel().isEmpty() || playerEntity.getLevel().get() >= currentLevel){
            return false;
        }
        int previousLevel = playerEntity.getLevel().get();
        int firstLevelPassedThrough = previousLevel + 1;
        //check all levels passed through to decide what milestone to congratulate on
        int thousands = 0;
        int hundreds = 0;
        int fifties = 0;
        int tens = 0;
        int difference = currentLevel - previousLevel;
        for(int i = firstLevelPassedThrough; i < currentLevel; i++){
            if(i % 1000 == 0) {
                thousands = i;
            }
            if(i % 100 == 0){
                hundreds = i;
            }
            if(i % 50 == 0) {
                fifties = i;
            }
            if(i % 10 == 0) {
                tens = i;
            }
        }
        final String postfix;
        if(wasInRoom){
            postfix = ".in-room";
        }else{
            postfix = ".on-join";
        }
        if(thousands > 0){
            chatController.send("welcome.milestone.thousands" + postfix, nickname, thousands);
        }else if (hundreds > 0){
            chatController.send("welcome.milestone.hundreds" + postfix, nickname, hundreds);
        }else if (fifties > 0){
            chatController.send("welcome.milestone.fifties" + postfix, nickname, fifties);
        }else if (tens > 0){
            chatController.send("welcome.milestone.tens" + postfix, nickname, tens);
        }else if (difference > 1){
            chatController.send("welcome.milestone.multiple" + postfix, nickname, previousLevel, currentLevel);
        }else{
            chatController.send("welcome.milestone.single" + postfix, nickname);
        }
        return true;
    }
}
