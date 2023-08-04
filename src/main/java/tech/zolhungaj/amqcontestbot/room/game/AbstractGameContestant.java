package tech.zolhungaj.amqcontestbot.room.game;

import lombok.Getter;
import lombok.Setter;

@Getter
public sealed class AbstractGameContestant implements GameContestant permits ContestantPlayer, ContestantTeam {
    @Setter
    private int score = 0;
    private int gameModeScore = 0;
    private int correctCount = 0;
    private int missCount = 0;
    @Setter
    private int position = -1;
    private long missTime = 0;
    private long correctTime = 0;


    @Override
    public void addGameModeScore(int add) {
        gameModeScore += add;
    }

    @Override
    public void incrementGameModeScore() {
        gameModeScore++;
    }

    @Override
    public void incrementCorrectCount() {
        correctCount++;
    }

    @Override
    public void incrementMissCount() {
        missCount++;
    }

    @Override
    public void addMissTime(long time) {
        this.missTime += time;
    }

    @Override
    public void addCorrectTime(long time) {
        correctTime += time;
    }


}
