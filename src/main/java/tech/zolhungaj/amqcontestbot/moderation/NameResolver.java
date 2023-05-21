package tech.zolhungaj.amqcontestbot.moderation;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tech.zolhungaj.amqapi.clientcommands.social.GetProfile;
import tech.zolhungaj.amqapi.servercommands.social.PlayerProfile;
import tech.zolhungaj.amqcontestbot.ApiManager;
import tech.zolhungaj.amqcontestbot.Util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class NameResolver {
    private static final int MAX_REQUESTS_PER_SECOND = 20;
    private final ApiManager api;
    private final Map<String, String> resolvedNames = new HashMap<>();
    private final ConcurrentLinkedQueue<String> pendingNames = new ConcurrentLinkedQueue<>();

    @PostConstruct
    private void init(){
        api.on(command -> {
            if(command instanceof PlayerProfile profile){
                resolvedNames.put(profile.nickname(), profile.originalName());
            }
            return true;
        });
    }

    /**
     * For general usage, most players will have already been resolved by Welcome and PunishmentManager
     * Simply blocks for 10 seconds hoping it works, if it doesn't, it throws a RuntimeException
     * */
    public String resolveOriginalName(String nickname){
        final CompletableFuture<String> future = resolveOriginalNameAsync(nickname);
        try{
            return future.get(10, TimeUnit.SECONDS);
        }catch(InterruptedException e){
            Thread.currentThread().interrupt();
            throw new NameResolutionFailedException(e);
        }catch (Exception e) {
            throw new NameResolutionFailedException(e);
        }
    }

    /**
     * For usage where there is a chance that the accessed player is not yet present*/
    public CompletableFuture<String> resolveOriginalNameAsync(String nickname){
        final CompletableFuture<String> future = new CompletableFuture<>();
        if(Util.isGuest(nickname)){
            //guests never have nicknames, and the profile lookup for guests is slow
            future.complete(nickname);
            return future;
        }
        if(resolvedNames.containsKey(nickname)){
            future.complete(resolvedNames.get(nickname));
            return future;
        }
        api.once(command -> {
            if(command instanceof PlayerProfile profile && (profile.nickname().equals(nickname))){
                future.complete(profile.originalName());
                return true;
            }
            return false;
        });
        queueNameResolve(nickname);
        return future;
    }

    private void queueNameResolve(String nickname){
        if(!pendingNames.contains(nickname)){
            pendingNames.add(nickname);
        }
    }

    @Scheduled(fixedDelay = 1000 / MAX_REQUESTS_PER_SECOND, timeUnit = TimeUnit.MILLISECONDS)
    public void resolveNames(){
        final String nickname = pendingNames.poll();
        if(nickname != null){
            api.sendCommand(new GetProfile(nickname));
        }
    }
}
