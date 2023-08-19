package tech.zolhungaj.amqcontestbot.database.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import tech.zolhungaj.amqcontestbot.database.enums.RulesetEnum;
import tech.zolhungaj.amqcontestbot.database.model.LeaderboardCountView;
import tech.zolhungaj.amqcontestbot.database.model.LeaderboardSpeedrunView;
import tech.zolhungaj.amqcontestbot.database.repository.LeaderboardCountRepository;
import tech.zolhungaj.amqcontestbot.database.repository.LeaderboardSpeedrunRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaderboardService {
    private final LeaderboardCountRepository countRepository;
    private final LeaderboardSpeedrunRepository speedrunRepository;

    public List<LeaderboardCountView> getCountLeaderboard(RulesetEnum rulesetEnum, int teamSize, int count){
        return countRepository.findAllByRulesetIsAndTeamSizeIsOrderByGameModeScoreDescTimesAchievedDescEarliestAchievedAsc(Pageable.ofSize(count),rulesetEnum, teamSize).toList();
    }

    public List<LeaderboardSpeedrunView> getSpeedrunLeaderboard(RulesetEnum rulesetEnum, int teamSize, int count) {
        return speedrunRepository.findAllByRulesetIsAndTeamSizeIsOrderByGameModeScoreDescCorrectTimeAscTimesAchievedDescEarliestAchievedAsc(Pageable.ofSize(count), rulesetEnum, teamSize).toList();
    }
}
