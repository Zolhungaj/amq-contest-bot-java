package tech.zolhungaj.amqcontestbot.gamemode;

import tech.zolhungaj.amqapi.sharedobjects.gamesettings.Vintage;


/**
 * Master of the Season is like Master of the Seasons, just with a player-selected season*/
public class MasterOfTheSeasonGameMode extends MasterOfTheSeasonsGameMode{

    private Vintage currentVintage = super.randomVintage();
    @Override
    protected Vintage randomVintage() {
        return currentVintage;
    }

    public void setCurrentVintage(Vintage currentVintage) {
        this.currentVintage = currentVintage;
    }
}
