package tech.zolhungaj.amqcontestbot.database.enums;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public enum RulesetEnum {
    OPENINGS,
    ENDINGS,
    INSERTS,
    OPENINGS_ENDINGS,
    ALL,
    ALL_HARD,
    MASTER_OF_THE_SEASON,
    MASTER_OF_SEASONS;
    private static final Map<String, RulesetEnum> MAP = new HashMap<>();
    private static final Map<RulesetEnum, List<String>> REVERSE_MAP = Map.of(
            OPENINGS, List.of("openings", "athena"),
            ENDINGS, List.of("endings", "ares"),
            INSERTS, List.of("inserts", "hestia"),
            OPENINGS_ENDINGS, List.of("openings_endings", "aphrodite"),
            ALL, List.of("all", "hades"),
            ALL_HARD, List.of("all_hard", "zeus"),
            MASTER_OF_THE_SEASON, List.of("master_of_the_season", "mots"),
            MASTER_OF_SEASONS, List.of("master_of_seasons", "mos", "hermes")
    );
    static {
        REVERSE_MAP.forEach((ruleset, aliases) -> aliases.forEach(alias -> MAP.put(alias, ruleset)));
        assert MAP.size() == values().length;
    }

    public static RulesetEnum fromName(String gameModeName) {
        return MAP.get(gameModeName.toLowerCase());
    }

    public static List<List<String>> allNames(){
        return Stream.of(values())
                .map(REVERSE_MAP::get)
                .toList();
    }
}
