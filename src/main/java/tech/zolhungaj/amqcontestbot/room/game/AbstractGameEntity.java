package tech.zolhungaj.amqcontestbot.room.game;

import lombok.Getter;

@Getter
public sealed class AbstractGameEntity implements GameEntity permits GamePlayer, GameTeam {
    private int score = 0;
    private int gameModeScore = 0;
    private int correctCount = 0;
    private int wrongCount = 0;

    @Override
    public void addScore(int add) {
        score += add;
    }

    @Override
    public void addGameModeScore(int add) {
        gameModeScore += add;
    }

    @Override
    public void incrementScore() {
        score++;
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
    public void incrementWrongCount() {
        wrongCount++;
    }


}
