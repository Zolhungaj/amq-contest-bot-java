package tech.zolhungaj.amqcontestbot.gamemode;

import tech.zolhungaj.amqcontestbot.database.enums.ScoringTypeEnum;
import tech.zolhungaj.amqcontestbot.room.game.GameContestant;

import java.util.*;

/// Like AbstractCountGameMode, but also takes into account the time it took to answer the question for ranking.
public abstract class AbstractSpeedrunGameMode extends AbstractCountGameMode{

    @Override
    protected final Comparator<GameContestant> comparator() {
        return Comparator
                .comparingInt(GameContestant::getScore)
                .thenComparingLong(GameContestant::getCorrectTime);
    }

    @Override
    public final ScoringTypeEnum scoringType() {
        return ScoringTypeEnum.SPEEDRUN;
    }
}
