package tech.zolhungaj.amqcontestbot.chat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tech.zolhungaj.amqapi.clientcommands.friend.FriendRequestResponse;
import tech.zolhungaj.amqapi.servercommands.social.FriendRequestReceived;
import tech.zolhungaj.amqcontestbot.ApiManager;

@Component
public class SocialManager {

    public SocialManager(@Autowired ApiManager api, @Autowired ChatCommands chatCommands){
        api.on(command -> {
            if(command instanceof FriendRequestReceived friendRequestReceived){
                api.sendCommand(new FriendRequestResponse(friendRequestReceived.playerName(), true));
            }
            return true;
        });
        chatCommands.register((sender, arguments) -> {
            //TODO: send friend request to sender
        }, "friend");
    }
}
