package tech.zolhungaj.amqcontestbot.moderation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tech.zolhungaj.amqcontestbot.ApiManager;

import java.util.concurrent.CompletableFuture;

@Component
public class NameResolver {

    private final ApiManager api;

    public NameResolver(@Autowired ApiManager api) {
        this.api = api;
    }

    /**
     * For general usage, most players will have already been resolved by Welcome and PunishmentManager
     * */
    public String getTrueNameBlocking(String nickname){
        return nickname; //TODO: implement
    }

    /**
     * For usage where there is a chance that the accessed player is not yet present*/
    public CompletableFuture<String> getTrueName(String nickname){
        CompletableFuture<String> future = new CompletableFuture<>();
        future.completeAsync(() -> getTrueNameBlocking(nickname));
        return future;
    }
}
