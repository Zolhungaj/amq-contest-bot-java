package tech.zolhungaj.amqcontestbot;

import java.util.regex.Pattern;

public class Util {
    private static final Pattern GUEST_MATCHER = Pattern.compile("^Guest-\\d{5}$");
    private Util(){}

    public static boolean isGuest(String playerName){
        return GUEST_MATCHER.matcher(playerName).matches();
    }
}
