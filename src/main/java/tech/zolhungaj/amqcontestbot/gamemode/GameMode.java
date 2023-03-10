package tech.zolhungaj.amqcontestbot.gamemode;

import tech.zolhungaj.amqapi.sharedobjects.gamesettings.GameSettings;

import java.util.Collection;

public sealed interface GameMode permits AbstractGameMode{
    GameSettings getNextSettings();
    void start(Collection<String> players);
    void score(Collection<Object> scoreObjects);
    Collection<PlayerScore> finish();
    record PlayerScore(
            String playerName,
            int position,
            int score,
            int correctCount,
            long correctTime,
            long time
    ){}
}
