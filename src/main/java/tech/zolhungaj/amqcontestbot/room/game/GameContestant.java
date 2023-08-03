package tech.zolhungaj.amqcontestbot.room.game;

public sealed interface GameContestant permits AbstractGameContestant {
    int getScore();
    void setScore(int score);
    int getGameModeScore();
    int getCorrectCount();
    int getWrongCount();
    void addGameModeScore(int add);
    void incrementGameModeScore();
    void incrementCorrectCount();
    void incrementWrongCount();
    int getPosition();
    void setPosition(int position);
    long getTime();
    void addTime(long time);
    long getCorrectTime();
    void addCorrectTime(long time);
}
