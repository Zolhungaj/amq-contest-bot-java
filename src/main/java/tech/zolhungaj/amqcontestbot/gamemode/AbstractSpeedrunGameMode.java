package tech.zolhungaj.amqcontestbot.gamemode;

import tech.zolhungaj.amqapi.servercommands.objects.PlayerAnswerResult;
import tech.zolhungaj.amqcontestbot.database.enums.ScoringTypeEnum;
import tech.zolhungaj.amqcontestbot.room.game.GameContestant;

import java.time.Duration;
import java.util.*;

public abstract class AbstractSpeedrunGameMode extends AbstractGameMode{

    @Override
    public final void score(GameContestant contestant, PlayerAnswerResult answerResult, Duration playerAnswerTime) {
        super.score(contestant, answerResult, playerAnswerTime);
        if(answerResult.correct()){
            contestant.incrementGameModeScore();
        }
    }

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