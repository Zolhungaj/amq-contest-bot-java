package tech.zolhungaj.amqcontestbot.database.enums;

public enum RulesetEnum {
    OPENINGS,
    ENDINGS,
    INSERTS,
    OPENINGS_ENDINGS,
    ALL,
    ALL_HARD,
    MASTER_OF_THE_SEASON,
    MASTER_OF_SEASONS;

    public static RulesetEnum fromName(String gameModeName) {
        return switch (gameModeName.toLowerCase()){
            case "openings", "athena" -> RulesetEnum.OPENINGS;
            case "endings", "ares" -> RulesetEnum.ENDINGS;
            case "inserts", "hestia" -> RulesetEnum.INSERTS;
            case "openings_endings", "aphrodite" -> RulesetEnum.OPENINGS_ENDINGS;
            case "all", "hades" -> RulesetEnum.ALL;
            case "all_hard", "zeus" -> RulesetEnum.ALL_HARD;
            case "master_of_the_season", "mots" -> RulesetEnum.MASTER_OF_THE_SEASON;
            case "master_of_seasons", "mos", "hermes" -> RulesetEnum.MASTER_OF_SEASONS;
            default -> null;
        };
    }
}
