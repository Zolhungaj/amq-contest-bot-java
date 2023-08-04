package tech.zolhungaj.amqcontestbot.gamemode;

import tech.zolhungaj.amqapi.servercommands.objects.PlayerAnswerResult;
import tech.zolhungaj.amqcontestbot.room.game.GameContestant;

import java.time.Duration;
import java.util.*;

public abstract non-sealed class AbstractGameMode implements GameMode{

    protected abstract Comparator<GameContestant> comparator();

    @Override
    public void score(GameContestant contestant, PlayerAnswerResult answerResult, Duration playerAnswerTime) {
        contestant.setScore(answerResult.score());
        if(answerResult.correct()){
            contestant.incrementCorrectCount();
        }else{
            contestant.incrementMissCount();
        }

        if(playerAnswerTime != null){
            long timeInMilliseconds = playerAnswerTime.toMillis();
            if(answerResult.correct()) {
                contestant.addCorrectTime(timeInMilliseconds);
            }else{
                contestant.addMissTime(timeInMilliseconds);
            }
        }
    }

    @Override
    public final void rank(Collection<GameContestant> contestants) {
        List<GameContestant> contestantsSorted = contestants
                .stream()
                .sorted(Collections.reverseOrder(this.comparator()))
                .toList();
        int position = 0;
        GameContestant previous = null;
        for(GameContestant contestant : contestantsSorted){
            if(previous == null || this.comparator().compare(previous, contestant) != 0){
                //since the list is sorted, all differences means a lower rank
                position++;
            }
            previous = contestant;
            contestant.setPosition(position);
        }
    }
}
