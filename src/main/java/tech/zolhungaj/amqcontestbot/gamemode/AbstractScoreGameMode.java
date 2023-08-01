package tech.zolhungaj.amqcontestbot.gamemode;

import tech.zolhungaj.amqcontestbot.database.enums.ScoringTypeEnum;

import java.util.Collection;
import java.util.Comparator;

public abstract class AbstractScoreGameMode extends AbstractGameMode {

    @Override
    public void score(Collection<Object> scoreObjects) {

    }

    @Override
    public final ScoringTypeEnum scoringType() {
        return ScoringTypeEnum.SCORE;
    }

    @Override
    protected final Comparator<PlayerScore> comparator() {
        return Comparator
                .comparingInt(PlayerScore::score);
    }
}
