package tech.zolhungaj.amqcontestbot.database.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.zolhungaj.amqcontestbot.database.enums.SongTypeEnum;
import tech.zolhungaj.amqcontestbot.database.model.AnimeEntity;
import tech.zolhungaj.amqcontestbot.database.model.SongEntity;

import java.util.Optional;

@Repository
public interface SongRepository extends JpaRepository<SongEntity, Integer> {
    Optional<SongEntity> findByAnimeIsAndTypeIsAndNumberIsAndTitleIsAndArtistIs(
            AnimeEntity anime,
            SongTypeEnum type,
            Integer number,
            String title,
            String artist);
}
