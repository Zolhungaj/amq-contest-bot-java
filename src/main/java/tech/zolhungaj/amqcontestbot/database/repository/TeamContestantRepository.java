package tech.zolhungaj.amqcontestbot.database.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.zolhungaj.amqcontestbot.database.model.TeamContestantEntity;
import tech.zolhungaj.amqcontestbot.database.model.TeamEntity;

import java.util.Optional;

@Repository
public interface TeamContestantRepository extends JpaRepository<TeamContestantEntity, Integer> {
    Optional<TeamContestantEntity> findByTeam(TeamEntity team);
}
