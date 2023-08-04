package tech.zolhungaj.amqcontestbot.room.game;

public sealed interface GameContestant permits AbstractGameContestant {
    int getScore();
    void setScore(int score);
    int getGameModeScore();
    int getCorrectCount();
    int getMissCount();
    void addGameModeScore(int add);
    void incrementGameModeScore();
    void incrementCorrectCount();
    void incrementMissCount();
    int getPosition();
    void setPosition(int position);
    long getMissTime();
    void addMissTime(long time);
    long getCorrectTime();
    void addCorrectTime(long time);
}
