package tech.zolhungaj.amqcontestbot.database.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tech.zolhungaj.amqcontestbot.database.model.PlayerEntity;
import tech.zolhungaj.amqcontestbot.database.repository.PlayerRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PlayerService {
    private final PlayerRepository repository;

    //For establishing tracking on a player not yet seen by the bot, currently only for bans
    public PlayerEntity getOrCreatePlayer(String originalName){
        Optional<PlayerEntity> player = getPlayer(originalName);
        if(player.isPresent()){
            return player.get();
        }else{
            PlayerEntity playerEntity = new PlayerEntity();
            playerEntity.setOriginalName(originalName);
            save(playerEntity);
            return playerEntity;
        }
    }
    public Optional<PlayerEntity> getPlayer(String originalName){
        return repository.findByOriginalNameIgnoreCase(originalName);
    }


    public void save(PlayerEntity playerEntity){
        repository.save(playerEntity);
    }
}
