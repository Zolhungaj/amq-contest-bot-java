package tech.zolhungaj.amqcontestbot.gamemode;

import java.util.Collection;

public sealed interface GameMode permits AbstractGameMode{
    Object getNextSettings();
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
