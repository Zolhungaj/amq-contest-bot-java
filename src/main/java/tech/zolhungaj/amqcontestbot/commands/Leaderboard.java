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
import tech.zolhungaj.amqcontestbot.database.service.ModerationService;
import tech.zolhungaj.amqcontestbot.exceptions.IncorrectArgumentCountException;
import tech.zolhungaj.amqcontestbot.gamemode.GameMode;
import tech.zolhungaj.amqcontestbot.moderation.NameResolver;
import tech.zolhungaj.amqcontestbot.room.lobby.LobbyStateManager;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
public class Leaderboard {
    private static final int MAX_LEADERBOARD_SIZE = 10;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mmz");
    private final ChatCommands commands;
    private final ChatController chat;
    private final LobbyStateManager lobbyStateManager;
    private final LeaderboardService leaderboardService;
    private final ModerationService moderationService;
    private final NameResolver nameResolver;

    @PostConstruct
    private void init(){
        commands.register((sender, arguments) -> {
            final int leaderboardSize;
            if((arguments.size() == 1 || arguments.size() == 4) && moderationService.isModerator(nameResolver.resolveOriginalName(sender))){
                leaderboardSize = Integer.parseInt(arguments.get(arguments.size() - 1));
            }else{
                leaderboardSize = MAX_LEADERBOARD_SIZE;
            }

            if(arguments.isEmpty() || arguments.size() == 1){
                GameMode gameMode = lobbyStateManager.getGameMode();
                printLeaderboard(gameMode.ruleset(), gameMode.scoringType(), gameMode.teamSize(), leaderboardSize);
            }else if(arguments.size() == 3 || arguments.size() == 4){
                RulesetEnum rulesetEnum = RulesetEnum.fromName(arguments.get(0));
                if(rulesetEnum == null){
                    chat.send("leaderboard.invalid-ruleset", arguments.get(0));
                    return;
                }
                ScoringTypeEnum scoringTypeEnum = ScoringTypeEnum.fromName(arguments.get(1));
                if(scoringTypeEnum == null){
                    chat.send("leaderboard.invalid-scoring-mode", arguments.get(1));
                    return;
                }
                int teamSize = Integer.parseInt(arguments.get(2));
                printLeaderboard(rulesetEnum, scoringTypeEnum, teamSize, leaderboardSize);
            }else{
                throw new IncorrectArgumentCountException(0, 1, 3, 4);
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
            String timesAchieved = countView.getTimesAchieved() == 1 ? "" : "×" + countView.getTimesAchieved();
            String earliestAchieved = countView.getEarliestAchieved().format(DATE_FORMATTER);
            chat.send("leaderboard.entry", position, contestantEntity.getName(), countView.getScoreRepresentation(), timesAchieved, earliestAchieved);
        }
    }
}
