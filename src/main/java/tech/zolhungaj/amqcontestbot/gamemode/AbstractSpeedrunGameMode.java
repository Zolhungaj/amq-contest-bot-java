package tech.zolhungaj.amqcontestbot.gamemode;

import tech.zolhungaj.amqcontestbot.database.enums.ScoringTypeEnum;

import java.util.*;

public abstract class AbstractSpeedrunGameMode extends AbstractGameMode{

    @Override
    public final void score(Collection<Object> scoreObjects) {

    }

    @Override
    protected final Comparator<PlayerScore> comparator() {
        return Comparator
                .comparingInt(PlayerScore::score)
                .thenComparingLong(PlayerScore::correctTime);
    }

    @Override
    public final ScoringTypeEnum scoringType() {
        return ScoringTypeEnum.SPEED;
    }
}
