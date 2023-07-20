package tech.zolhungaj.amqcontestbot.database.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.zolhungaj.amqcontestbot.database.model.AnimeEntity;

public interface AnimeRepository extends JpaRepository<AnimeEntity, Integer> {
}
