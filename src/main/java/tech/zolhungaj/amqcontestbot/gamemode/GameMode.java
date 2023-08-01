package tech.zolhungaj.amqcontestbot.gamemode;

import lombok.With;
import tech.zolhungaj.amqapi.sharedobjects.gamesettings.GameSettings;
import tech.zolhungaj.amqcontestbot.database.enums.RulesetEnum;
import tech.zolhungaj.amqcontestbot.database.enums.ScoringTypeEnum;

import java.util.Collection;

public sealed interface GameMode permits AbstractGameMode{
    GameSettings getNextSettings();
    void start(Collection<String> players);
    void score(Collection<Object> scoreObjects);
    Collection<PlayerScore> finish();
    @With //replace with GameContestant?
    record PlayerScore(
            String playerName,
            int position,
            int score,
            int correctCount,
            long correctTime,
            long time
    ){}

    RulesetEnum ruleset();
    ScoringTypeEnum scoringType();

    default boolean sameGameMode(GameMode gameMode){
        if(gameMode == null) return false;
        return this.ruleset() == gameMode.ruleset() && this.scoringType() == gameMode.scoringType();
    }
}
