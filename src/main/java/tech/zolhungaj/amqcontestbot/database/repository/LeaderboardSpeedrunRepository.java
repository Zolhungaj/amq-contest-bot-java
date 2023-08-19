package tech.zolhungaj.amqcontestbot.database.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.zolhungaj.amqcontestbot.database.enums.RulesetEnum;
import tech.zolhungaj.amqcontestbot.database.model.LeaderboardSpeedrunView;

@Repository
public interface LeaderboardSpeedrunRepository extends JpaRepository<LeaderboardSpeedrunView, LeaderboardSpeedrunView.LeaderboardId> {
    Page<LeaderboardSpeedrunView> findAllByRulesetIsAndTeamSizeIsOrderByGameModeScoreDescCorrectTimeAscTimesAchievedDescEarliestAchievedAsc(Pageable pageable, RulesetEnum rulesetEnum, int teamSize);
}
