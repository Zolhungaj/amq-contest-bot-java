package tech.zolhungaj.amqcontestbot.database.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.zolhungaj.amqcontestbot.database.model.GameEntity;
import tech.zolhungaj.amqcontestbot.database.model.GameSongEntity;

import java.util.Optional;

@Repository
public interface GameSongRepository extends JpaRepository<GameSongEntity, Integer> {
    Optional<GameSongEntity> findFirstByGameOrderByOrdinalDesc(GameEntity game);
}
