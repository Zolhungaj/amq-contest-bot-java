package tech.zolhungaj.amqcontestbot.gamemode;

import tech.zolhungaj.amqcontestbot.database.enums.ScoringTypeEnum;

import java.util.Collection;

public abstract class AbstractScoreGameMode extends AbstractGameMode {
    @Override
    protected void reset() {

    }

    @Override
    protected void init(Collection<String> players) {

    }

    @Override
    public void score(Collection<Object> scoreObjects) {

    }

    @Override
    public Collection<PlayerScore> finish() {
        return null;
    }

    @Override
    public final ScoringTypeEnum scoringType() {
        return ScoringTypeEnum.SCORE;
    }
}
