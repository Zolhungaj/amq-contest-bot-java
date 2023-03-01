package tech.zolhungaj.amqcontestbot.repository;

import org.springframework.stereotype.Service;

@Service
public class PlayerService {
    public void ban(String trueName){
        //TODO:implement
    }
    public void unban(String trueName){
        //TODO:implement
    }
    public boolean isBanned(String trueName){
        return false; //TODO:implement
    }

    public boolean isModerator(String trueName){
        return false;//TODO:implement
    }
    public boolean isAdmin(String trueName){
        return false;//TODO:implement
    }

    public Object getPlayer(String trueName){
        return new Object();
    }
}
