package tech.zolhungaj.amqcontestbot.room;

public sealed interface GameEntity permits AbstractGameEntity {
    int getScore();
    int getGameModeScore();
    int getCorrectCount();
    int getWrongCount();
    void addScore(int add);
    void addGameModeScore(int add);
    void incrementScore();
    void incrementGameModeScore();
    void incrementCorrectCount();
    void incrementWrongCount();
}
