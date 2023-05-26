package tech.zolhungaj.amqcontestbot.chat;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tech.zolhungaj.amqapi.clientcommands.social.RespondToFriendRequest;
import tech.zolhungaj.amqapi.clientcommands.social.SendFriendRequest;
import tech.zolhungaj.amqapi.servercommands.social.FriendRequestReceived;
import tech.zolhungaj.amqcontestbot.ApiManager;

@Component
@RequiredArgsConstructor
public class FriendModule {
    private final ApiManager api;
    private final ChatCommands chatCommands;

    @PostConstruct
    public void init(){
        api.on(FriendRequestReceived.class, friendRequest -> acceptFriendRequest(friendRequest.playerName()));
        chatCommands.register((sender, arguments) -> sendFriendRequest(sender), "friend");
    }

    public void sendFriendRequest(String name){
        api.sendCommand(new SendFriendRequest(name));
    }

    public void acceptFriendRequest(String name){
        api.sendCommand(new RespondToFriendRequest(name, true));
    }
}
