package tech.zolhungaj.amqcontestbot.gamemode;

import lombok.NonNull;
import tech.zolhungaj.amqapi.sharedobjects.gamesettings.*;
import tech.zolhungaj.amqcontestbot.database.enums.RulesetEnum;
import tech.zolhungaj.amqcontestbot.database.enums.ScoringTypeEnum;

public class GameModeFactory {
    private static final int SONG_COUNT = 100;
    private static final GameSettings BASE_RANDOM_SETTINGS = GameSettings.DEFAULT.toBuilder()
            .guessTime(new GuessTime(15))
            .numberOfSongs(SONG_COUNT)
            .songSelection(SongSelection.of(SongSelection.SelectionIdentifier.RANDOM, SONG_COUNT))
            .roomSize(40)
            .songTypeSelection(SongTypeSelection.of(SONG_COUNT, SongTypeSelection.SongType.ALL))
            .build();
    public static GameMode getGameMode(@NonNull RulesetEnum ruleset, @NonNull ScoringTypeEnum scoringType){
        return getGameMode(ruleset, scoringType, 1);
    }
    public static GameMode getGameMode(@NonNull RulesetEnum ruleset, @NonNull ScoringTypeEnum scoringType, int teamSize) {
        if(ruleset == RulesetEnum.MASTER_OF_THE_SEASON){
            if(scoringType != ScoringTypeEnum.COUNT){
                throw new IllegalArgumentException("Master of the Season only supports COUNT scoring");
            }
            return new MasterOfTheSeasonGameMode(){
                @Override
                public GameSettings getNextSettings() {
                    return super.getNextSettings().withTeamSize(teamSize);
                }
            };
        }
        if(ruleset == RulesetEnum.MASTER_OF_SEASONS){
            if(scoringType != ScoringTypeEnum.COUNT){
                throw new IllegalArgumentException("Master of Seasons only supports COUNT scoring");
            }
            return new MasterOfSeasonsGameMode(){
                @Override
                public GameSettings getNextSettings() {
                    return super.getNextSettings().withTeamSize(teamSize);
                }
            };
        }
        GameSettings baseSettings = switch (ruleset){
            case OPENINGS -> BASE_RANDOM_SETTINGS.withSongTypeSelection(SongTypeSelection.of(SONG_COUNT, SongTypeSelection.SongType.OPENINGS));
            case ENDINGS -> BASE_RANDOM_SETTINGS.withSongTypeSelection(SongTypeSelection.of(SONG_COUNT, SongTypeSelection.SongType.ENDINGS));
            case INSERTS -> BASE_RANDOM_SETTINGS.withSongTypeSelection(SongTypeSelection.of(SONG_COUNT, SongTypeSelection.SongType.INSERTS));
            case OPENINGS_ENDINGS -> BASE_RANDOM_SETTINGS.withSongTypeSelection(SongTypeSelection.of(SONG_COUNT, SongTypeSelection.SongType.OPENINGS, SongTypeSelection.SongType.ENDINGS));
            case ALL -> BASE_RANDOM_SETTINGS.withSongTypeSelection(SongTypeSelection.of(SONG_COUNT, SongTypeSelection.SongType.ALL));
            case ALL_HARD -> BASE_RANDOM_SETTINGS.withSongTypeSelection(SongTypeSelection.of(SONG_COUNT, SongTypeSelection.SongType.ALL)).withSongDifficulty(SongDifficulty.of(0, 40));
            case MASTER_OF_THE_SEASON, MASTER_OF_SEASONS -> throw new IllegalStateException(); // Already handled above, for the compiler
        };
        String roomName = switch (ruleset){
            case OPENINGS -> "Athena";
            case ENDINGS -> "EDs";
            case INSERTS -> "INs";
            case OPENINGS_ENDINGS -> "Classic";
            case ALL -> "All";
            case ALL_HARD -> "Zeus";
            case MASTER_OF_THE_SEASON, MASTER_OF_SEASONS -> throw new IllegalStateException(); // Already handled above, for the compiler
        }
        + switch (scoringType){
            case COUNT -> "";
            case SPEEDRUN -> " Speedrun";
            case SPEED -> " QuickDraw";
            case LIVES -> " Lives";
        };

        GameSettings settings = baseSettings.withTeamSize(teamSize).withRoomName(roomName);
        return switch (scoringType){
            case COUNT -> new AbstractCountGameMode() {
                @Override
                public GameSettings getNextSettings() {
                    return settings.withScoreType(GameSettings.ScoreType.COUNT.value);
                }

                @Override
                public RulesetEnum ruleset() {
                    return ruleset;
                }
            };
            case SPEEDRUN -> new AbstractSpeedrunGameMode() {
                @Override
                public GameSettings getNextSettings() {
                    return settings.withScoreType(GameSettings.ScoreType.COUNT.value);
                }

                @Override
                public RulesetEnum ruleset() {
                    return ruleset;
                }
            };
            case SPEED, LIVES -> throw new IllegalArgumentException("Not implemented yet");
        };
    }


}
