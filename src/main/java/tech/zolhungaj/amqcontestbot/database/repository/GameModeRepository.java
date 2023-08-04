package tech.zolhungaj.amqcontestbot.database.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.zolhungaj.amqcontestbot.database.enums.RulesetEnum;
import tech.zolhungaj.amqcontestbot.database.enums.ScoringTypeEnum;
import tech.zolhungaj.amqcontestbot.database.model.GameModeEntity;

import java.util.Optional;

@Repository
public interface GameModeRepository extends JpaRepository<GameModeEntity, Integer> {
    Optional<GameModeEntity> findByRulesetAndScoringTypeAndTeamSize(RulesetEnum ruleset, ScoringTypeEnum scoringType, int teamSize);
}
