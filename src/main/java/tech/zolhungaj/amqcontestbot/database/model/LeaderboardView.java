package tech.zolhungaj.amqcontestbot.database.model;

import tech.zolhungaj.amqcontestbot.database.enums.RulesetEnum;

import java.time.OffsetDateTime;

public interface LeaderboardView extends Comparable<LeaderboardView>{
    ContestantEntity getContestant();
    RulesetEnum getRuleset();
    int getTeamSize();
    int getTimesAchieved();
    OffsetDateTime getEarliestAchieved();
    String getScoreRepresentation();
    String getName();
}
