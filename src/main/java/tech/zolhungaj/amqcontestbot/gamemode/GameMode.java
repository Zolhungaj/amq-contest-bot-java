package tech.zolhungaj.amqcontestbot.gamemode;

import lombok.With;
import tech.zolhungaj.amqapi.servercommands.objects.PlayerAnswerResult;
import tech.zolhungaj.amqapi.sharedobjects.gamesettings.GameSettings;
import tech.zolhungaj.amqcontestbot.database.enums.RulesetEnum;
import tech.zolhungaj.amqcontestbot.database.enums.ScoringTypeEnum;
import tech.zolhungaj.amqcontestbot.room.game.GameContestant;

import java.time.Duration;
import java.util.Collection;

public sealed interface GameMode permits AbstractGameMode{
    GameSettings getNextSettings();
    void score(GameContestant contestant, PlayerAnswerResult answerResult, Duration playerAnswerTime);
    void rank(Collection<GameContestant> contestants);
    RulesetEnum ruleset();
    ScoringTypeEnum scoringType();

    default boolean sameGameMode(GameMode gameMode){
        if(gameMode == null) return false;
        return this.ruleset() == gameMode.ruleset() && this.scoringType() == gameMode.scoringType();
    }
}
