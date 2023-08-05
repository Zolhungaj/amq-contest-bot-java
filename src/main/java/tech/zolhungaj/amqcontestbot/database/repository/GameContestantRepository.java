package tech.zolhungaj.amqcontestbot.database.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.zolhungaj.amqcontestbot.database.model.GameContestantEntity;

@Repository
public interface GameContestantRepository extends JpaRepository<GameContestantEntity, Integer> {

}
