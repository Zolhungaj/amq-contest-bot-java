package tech.zolhungaj.amqcontestbot.commands;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tech.zolhungaj.amqcontestbot.chat.ChatController;
import tech.zolhungaj.amqcontestbot.database.enums.RulesetEnum;
import tech.zolhungaj.amqcontestbot.database.enums.ScoringTypeEnum;
import tech.zolhungaj.amqcontestbot.database.model.ContestantEntity;
import tech.zolhungaj.amqcontestbot.database.model.LeaderboardView;
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
            case COUNT -> printLeaderboard(leaderboardService.getCountLeaderboard(rulesetEnum, teamSize, count));
            case SPEEDRUN -> printLeaderboard(leaderboardService.getSpeedrunLeaderboard(rulesetEnum, teamSize, count));
            default -> chat.send("leaderboard.not-implemented", scoringTypeEnum.name());
        }
    }

    private void printLeaderboard(List<? extends LeaderboardView> leaderboard){
        int position = 1;
        LeaderboardView previous = null;
        for(LeaderboardView countView : leaderboard){
            if(previous != null && countView.compareTo(previous) != 0){
                position++;
            }
            previous = countView;
            ContestantEntity contestantEntity = countView.getContestant();
            String timesAchieved = countView.getTimesAchieved() == 1 ? "" : "x" + countView.getTimesAchieved();
            chat.send("leaderboard.entry", position, contestantEntity.getName(), countView.getScoreRepresentation(), timesAchieved, countView.getEarliestAchieved());
        }
    }
}
