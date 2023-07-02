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
    public Optional<PlayerEntity> getPlayer(String originalName){
        return repository.findByOriginalNameIgnoreCase(originalName);
    }
}
