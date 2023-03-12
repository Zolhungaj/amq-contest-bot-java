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
        if("Zolhungaj".equals(trueName)){
            return true; //TODO: remove lol
        }
        return false;//TODO:implement
    }

    public void addAdmin(String source, String newAdmin){
        //TODO: implement
    }

    public void removeAdmin(String remover, String admin){
        //TODO: implement
    }

    public void addModerator(String source, String newModerator){
        //TODO: implement
    }

    public void removeModerator(String remover, String moderator){
        //TODO: implement
    }

    public Object getPlayer(String trueName){
        return new Object();
    }
}
