package tech.zolhungaj.amqcontestbot.moderation;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tech.zolhungaj.amqapi.clientcommands.social.GetProfile;
import tech.zolhungaj.amqcontestbot.ApiManager;
import tech.zolhungaj.amqcontestbot.Util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class NameResolver {
    private final ApiManager api;
    private final Map<String, String> resolvedNames = new HashMap<>();

    @PostConstruct
    private void init(){
        api.on(command -> {
//            if(command instanceof ProfileEvent profile){
//                resolvedNames.put(profile.nickname(), profile.trueName());
//            }
            return true;
        });
    }

    /**
     * For general usage, most players will have already been resolved by Welcome and PunishmentManager
     * */
    public String getTrueNameBlocking(String nickname){
        if(Util.isGuest(nickname)){
            //guests never have nicknames, and the profile lookup for guests is slow
            return nickname;
        }
        if(resolvedNames.containsKey(nickname)){
            return resolvedNames.get(nickname);
        }
        api.sendCommand(new GetProfile(nickname));
        CompletableFuture<String> future = new CompletableFuture<>();
        api.once(command -> {
//            if(command instanceof ProfileEvent profile && (profile.nickname().equals(nickname))){
//                    future.complete(profile.trueName());
//                    return true;
//            }
            return false;
        });
        future.complete(nickname); //TODO: remove this line when ProfileEvent is implemented
        try{
            return future.get(10, TimeUnit.SECONDS);
        }catch (Exception e) {
            //TODO: handle this exception better
            throw new RuntimeException(e);
        }
    }

    /**
     * For usage where there is a chance that the accessed player is not yet present*/
    public CompletableFuture<String> getTrueName(String nickname){
        CompletableFuture<String> future = new CompletableFuture<>();
        future.completeAsync(() -> getTrueNameBlocking(nickname));
        return future;
    }
}
