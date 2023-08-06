package tech.zolhungaj.amqcontestbot.gamemode;

import tech.zolhungaj.amqcontestbot.database.enums.ScoringTypeEnum;
import tech.zolhungaj.amqcontestbot.room.game.GameContestant;

import java.util.Comparator;

public abstract class AbstractCountGameMode extends AbstractGameMode {

    @Override
    public final void score(GameContestant contestant, AnswerResult answerResult) {
        super.score(contestant, answerResult);
        if(answerResult.correct()){
            contestant.incrementGameModeScore();
        }
    }

    @Override
    public ScoringTypeEnum scoringType() {
        return ScoringTypeEnum.COUNT;
    }

    @Override
    protected Comparator<GameContestant> comparator() {
        return Comparator
                .comparingInt(GameContestant::getGameModeScore);
    }
}
