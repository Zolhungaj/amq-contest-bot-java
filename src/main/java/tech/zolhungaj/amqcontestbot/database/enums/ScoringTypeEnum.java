package tech.zolhungaj.amqcontestbot.database.enums;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public enum ScoringTypeEnum {
    COUNT,
    SPEEDRUN,
    SPEED,
    LIVES;

    private static final Map<String, ScoringTypeEnum> MAP = new HashMap<>();
    static{
        for (ScoringTypeEnum scoringType : values()) {
            MAP.put(scoringType.name().toLowerCase(), scoringType);
        }
        assert MAP.size() == values().length;
    }

    public static ScoringTypeEnum fromName(String scoringMode) {
        return MAP.get(scoringMode.toLowerCase());
    }

    public static List<String> allNames(){
        return Stream.of(values()).map(Enum::name).map(String::toLowerCase).toList();
    }
}
