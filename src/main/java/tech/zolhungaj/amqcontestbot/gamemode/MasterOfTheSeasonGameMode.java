package tech.zolhungaj.amqcontestbot.gamemode;

import tech.zolhungaj.amqapi.sharedobjects.gamesettings.Vintage;
import tech.zolhungaj.amqcontestbot.database.enums.RulesetEnum;


/**
 * Master of the Season is like Master of Seasons, just with a player-selected season*/
public final class MasterOfTheSeasonGameMode extends MasterOfSeasonsGameMode {

    private Vintage currentVintage = super.randomVintage();
    @Override
    protected Vintage randomVintage() {
        return currentVintage;
    }

    @Override
    protected String roomPrefix() {
        return "MasterOfTheSeason";
    }

    public void setCurrentVintage(Vintage currentVintage) {
        this.currentVintage = currentVintage;
    }


    @Override
    public RulesetEnum ruleset() {
        return RulesetEnum.MASTER_OF_THE_SEASON;
    }
}
