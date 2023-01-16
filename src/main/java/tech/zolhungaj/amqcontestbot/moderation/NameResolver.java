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

    public String getTrueNameBlocking(String nickname){
        return nickname; //TODO: implement
    }

    public CompletableFuture<String> getTrueName(String nickname){
        CompletableFuture<String> future = new CompletableFuture<>();
        future.completeAsync(() -> getTrueNameBlocking(nickname));
        return future;
    }
}
