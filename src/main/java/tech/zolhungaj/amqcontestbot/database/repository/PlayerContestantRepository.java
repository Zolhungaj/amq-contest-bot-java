package tech.zolhungaj.amqcontestbot.database.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.zolhungaj.amqcontestbot.database.model.PlayerContestantEntity;
import tech.zolhungaj.amqcontestbot.database.model.PlayerEntity;

import java.util.Optional;

@Repository
public interface PlayerContestantRepository extends JpaRepository<PlayerContestantEntity, Integer> {
    Optional<PlayerContestantEntity> findByPlayer(PlayerEntity team);
}
