package tech.zolhungaj.amqcontestbot.database.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ModerationService {
    private final PlayerService playerService;
    public void ban(String originalName){
        //TODO:implement
    }
    public void unban(String originalName){
        //TODO:implement
    }
    public boolean isBanned(String originalName){
        return false; //TODO:implement
    }

    public boolean isModerator(String originalName){
        return false;//TODO:implement
    }
    public boolean isAdmin(String originalName){
        if("Zolhungaj".equals(originalName)){
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
}
