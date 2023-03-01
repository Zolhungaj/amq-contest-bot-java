package tech.zolhungaj.amqcontestbot.chat;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tech.zolhungaj.amqapi.clientcommands.friend.FriendRequestResponse;
import tech.zolhungaj.amqapi.servercommands.social.FriendRequestReceived;
import tech.zolhungaj.amqcontestbot.ApiManager;

@Component
@RequiredArgsConstructor
public class FriendModule {
    private final ApiManager api;
    private final ChatCommands chatCommands;

    @PostConstruct
    public void init(){
        api.on(command -> {
            if(command instanceof FriendRequestReceived friendRequestReceived){
                acceptFriendRequest(friendRequestReceived.playerName());
            }
            return true;
        });
        chatCommands.register((sender, arguments) -> sendFriendRequest(sender), "friend");
    }

    public void sendFriendRequest(String name){
        //TODO: send friend request
    }

    public void acceptFriendRequest(String name){
        api.sendCommand(new FriendRequestResponse(name, true));
    }
}
