package tech.zolhungaj.amqcontestbot.moderation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tech.zolhungaj.amqcontestbot.ApiManager;

@Component
public class NameResolver {

    private final ApiManager api;

    public NameResolver(@Autowired ApiManager api) {
        this.api = api;
    }

    public String getTrueName(String nickname){
        return nickname; //TODO: implement
    }
}
