package tech.zolhungaj.amqcontestbot.database.enums;

public enum ScoringTypeEnum {
    COUNT,
    SPEEDRUN,
    SPEED,
    LIVES;

    public static ScoringTypeEnum fromName(String scoringMode) {
        return switch (scoringMode.toLowerCase()){
            case "count" -> ScoringTypeEnum.COUNT;
            case "speedrun" -> ScoringTypeEnum.SPEEDRUN;
            case "speed" -> ScoringTypeEnum.SPEED;
            case "lives" -> ScoringTypeEnum.LIVES;
            default -> null;
        };
    }
}
