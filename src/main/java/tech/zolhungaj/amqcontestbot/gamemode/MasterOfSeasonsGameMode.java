package tech.zolhungaj.amqcontestbot.gamemode;

import tech.zolhungaj.amqapi.sharedobjects.gamesettings.*;
import tech.zolhungaj.amqcontestbot.database.enums.RulesetEnum;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.random.RandomGenerator;

public abstract class MasterOfSeasonsGameMode extends AbstractCountGameMode {
    private static final int SONG_COUNT = 20;
    private static final GameSettings BASE_GAME_SETTINGS = GameSettings.DEFAULT
            .toBuilder()
            .guessTime(new GuessTime(15))
            .numberOfSongs(SONG_COUNT)
            .songSelection(SongSelection.of(SongSelection.SelectionIdentifier.RANDOM, SONG_COUNT))
            .roomSize(100)
            .songTypeSelection(SongTypeSelection.of(SONG_COUNT, SongTypeSelection.SongType.ALL))
            .build();
    private final RandomGenerator randomGenerator = new Random();
    @Override
    public GameSettings getNextSettings() {
        Vintage vintage = randomVintage();
        int year = vintage.seasonRange().years().get(0) % 100;
        int seasonValue = vintage.seasonRange().seasons().get(0);
        final String season;
        if(seasonValue == Vintage.Season.WINTER.value){
            season = "冬";
        }else if(seasonValue == Vintage.Season.SPRING.value){
            season = "春";
        }else if(seasonValue == Vintage.Season.SUMMER.value){
            season = "夏";
        }else if(seasonValue == Vintage.Season.AUTUMN.value){
            season = "秋";
        }else{
            season = "憂";
        }
        String roomName = roomPrefix() + "%02d%s".formatted(year, season);
        return BASE_GAME_SETTINGS.toBuilder()
                .vintage(vintage)
                .roomName(roomName)
                .build();
    }

    protected Vintage randomVintage(){
        List<Vintage> seasons = allValidSeasons();
        int index = randomGenerator.nextInt(seasons.size());
        return seasons.get(index);
    }

    protected String roomPrefix(){
        return "Hermes of Seasons";
    }
    /**
     * Returns all valid seasons:
     * Criteria for validity:
     *  Any season from a previous year
     *  Any season from the current year, where at least two months have passed
     * */
    private List<Vintage> allValidSeasons(){
        List<Vintage.Season> seasons = List.of(
                Vintage.Season.WINTER,
                Vintage.Season.SPRING,
                Vintage.Season.SUMMER,
                Vintage.Season.AUTUMN);
        List<Vintage> vintages = new ArrayList<>();
        for(int year = Vintage.MIN_YEAR; year < Vintage.MAX_YEAR; year++){
            for(Vintage.Season season : seasons){
                vintages.add(Vintage.of(year, season, year, season));
            }
        }
        LocalDate now = LocalDate.now();
        LocalDate winter = LocalDate.of(Vintage.MAX_YEAR, 2, 1);
        LocalDate spring = winter.plusMonths(3);
        LocalDate summer = spring.plusMonths(3);
        LocalDate autumn = summer.plusMonths(3);

        if(winter.isBefore(now)){
            vintages.add(Vintage.of(Vintage.MAX_YEAR, Vintage.Season.WINTER, Vintage.MAX_YEAR, Vintage.Season.WINTER));
        }
        if(spring.isBefore(now)){
            vintages.add(Vintage.of(Vintage.MAX_YEAR, Vintage.Season.SPRING, Vintage.MAX_YEAR, Vintage.Season.SPRING));
        }
        if(summer.isBefore(now)){
            vintages.add(Vintage.of(Vintage.MAX_YEAR, Vintage.Season.SUMMER, Vintage.MAX_YEAR, Vintage.Season.SUMMER));
        }
        if(autumn.isBefore(now)){
            vintages.add(Vintage.of(Vintage.MAX_YEAR, Vintage.Season.AUTUMN, Vintage.MAX_YEAR, Vintage.Season.AUTUMN));
        }

        return vintages;
    }

    @Override
    public RulesetEnum ruleset() {
        return RulesetEnum.MASTER_OF_SEASONS;
    }
}
