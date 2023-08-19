package tech.zolhungaj.amqcontestbot.chat;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tech.zolhungaj.amqcontestbot.database.enums.RulesetEnum;
import tech.zolhungaj.amqcontestbot.database.enums.ScoringTypeEnum;
import tech.zolhungaj.amqcontestbot.database.model.*;
import tech.zolhungaj.amqcontestbot.database.service.LeaderboardService;
import tech.zolhungaj.amqcontestbot.gamemode.GameMode;
import tech.zolhungaj.amqcontestbot.room.lobby.LobbyStateManager;

import java.util.List;

@Component
@RequiredArgsConstructor
public class Leaderboard {
    private static final int MAX_LEADERBOARD_SIZE = 10;
    private final ChatCommands commands;
    private final ChatController chat;
    private final LobbyStateManager lobbyStateManager;
    private final LeaderboardService leaderboardService;

    @PostConstruct
    private void init(){
        commands.register((sender, arguments) -> {
            if(arguments.isEmpty()){
                GameMode gameMode = lobbyStateManager.getGameMode();
                printLeaderboard(gameMode.ruleset(), gameMode.scoringType(), gameMode.teamSize(), MAX_LEADERBOARD_SIZE);
            }

        }, "leaderboard", "lb");
    }

    private void printLeaderboard(RulesetEnum rulesetEnum, ScoringTypeEnum scoringTypeEnum, int teamSize, int count){
        chat.send("leaderboard.start", rulesetEnum.name(), scoringTypeEnum.name(), teamSize, count);
        switch (scoringTypeEnum){
            case COUNT -> printLeaderboardCount(rulesetEnum, teamSize, count);
            default -> chat.send("leaderboard.not_implemented", scoringTypeEnum.name());
        }
    }

    private void printLeaderboardCount(RulesetEnum rulesetEnum, int teamSize, int count){
        List<LeaderboardCountView> leaderboard = leaderboardService.getCountLeaderboard(rulesetEnum, teamSize, count);
        leaderboard.forEach(countView -> {
            ContestantEntity contestantEntity = countView.getContestant();
            chat.send("leaderboard.count.entry", contestantEntity.getName(), countView.getGameModeScore(), countView.getTimesAchieved(), countView.getEarliestAchieved());
        });
    }
}
