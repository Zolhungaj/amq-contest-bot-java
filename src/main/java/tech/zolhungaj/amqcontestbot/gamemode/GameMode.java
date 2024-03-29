package tech.zolhungaj.amqcontestbot.gamemode;

import tech.zolhungaj.amqapi.sharedobjects.gamesettings.GameSettings;
import tech.zolhungaj.amqcontestbot.database.enums.RulesetEnum;
import tech.zolhungaj.amqcontestbot.database.enums.ScoringTypeEnum;
import tech.zolhungaj.amqcontestbot.room.game.GameContestant;

import java.util.Collection;

public sealed interface GameMode permits AbstractGameMode{
    GameSettings getNextSettings();
    void score(GameContestant contestant, AnswerResult answerResult);
    void rank(Collection<GameContestant> contestants);
    RulesetEnum ruleset();
    ScoringTypeEnum scoringType();
    int teamSize();

    default boolean sameGameMode(GameMode gameMode){
        if(gameMode == null) return false;
        return this.ruleset() == gameMode.ruleset() && this.scoringType() == gameMode.scoringType() && this.teamSize() == gameMode.teamSize();
    }
}
