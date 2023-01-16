package tech.zolhungaj.amqcontestbot.chat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tech.zolhungaj.amqapi.servercommands.gameroom.SpectatorJoined;
import tech.zolhungaj.amqcontestbot.ApiManager;
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
    private final NameResolver nameResolver;
    private final PlayerService playerService;
    public QuipGenerator(@Autowired ChatCommands chatCommands,
                         @Autowired PlayerService playerService,
                         @Autowired ApiManager api,
                         @Autowired NameResolver nameResolver,
                         @Autowired ChatController chatController){
        this.chatController = chatController;
        this.nameResolver = nameResolver;
        this.playerService = playerService;
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
        api.on(command -> {
            if(command instanceof SpectatorJoined spectatorJoined){
                newSpectator(spectatorJoined.playerName());
            }
            return true;
        });
    }

    private void quipPlaceholder(){
        if(randomGenerator.nextDouble() >= (1.0 - chattiness)){
            chatController.send("quip");
        }
    }

    private void newPlayer(){

    }

    private void newSpectator(String nickname){
        nameResolver
                .getTrueName(nickname)
                .thenAccept(trueName -> newSpectator(nickname, trueName));
    }
    private void newSpectator(String nickname, String trueName){
        Object player = playerService.getPlayer(trueName);
        //TODO: handle the rest
    }
}
