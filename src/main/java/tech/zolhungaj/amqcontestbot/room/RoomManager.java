package tech.zolhungaj.amqcontestbot.room;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tech.zolhungaj.amqapi.clientcommands.roombrowser.HostGame;
import tech.zolhungaj.amqapi.sharedobjects.gamesettings.GameSettings;
import tech.zolhungaj.amqcontestbot.ApiManager;
import tech.zolhungaj.amqcontestbot.gamemode.GameMode;
import tech.zolhungaj.amqcontestbot.gamemode.MasterOfTheSeasonsGameMode;

import java.util.concurrent.TimeUnit;

@Component
@EnableScheduling
@RequiredArgsConstructor
public class RoomManager {
    private final ApiManager api;
    private final GameMode gameMode = new MasterOfTheSeasonsGameMode();
    private GameSettings currentSettings = null;



    @Scheduled(fixedRate = 1, initialDelay = 5, timeUnit = TimeUnit.SECONDS)
    public void updateState(){
        if(currentSettings == null){
            currentSettings = gameMode.getNextSettings();
            api.sendCommand(new HostGame(currentSettings));
        }
    }
}
