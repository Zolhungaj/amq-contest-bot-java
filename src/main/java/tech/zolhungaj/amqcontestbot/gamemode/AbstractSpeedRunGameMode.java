package tech.zolhungaj.amqcontestbot.gamemode;

import lombok.Data;
import lombok.NonNull;

import java.util.*;

public abstract class AbstractSpeedRunGameMode extends AbstractGameMode{

    private final Map<String, Player> playerMap = new HashMap<>();
    @Override
    protected final void reset() {
        playerMap.clear();
    }

    @Override
    protected final void init(Collection<String> players) {
        players.forEach(player -> playerMap.put(player, new Player(player)));
    }

    @Override
    public final void score(Collection<Object> scoreObjects) {

    }

    @Override
    public final Collection<GameMode.PlayerScore> finish() {
        List<Player> players =  playerMap.values()
                .stream()
                .sorted(Collections.reverseOrder())
                .toList();
        int position = 0;
        int lastScore = Integer.MAX_VALUE;
        long lastTime = Long.MAX_VALUE;
        List<PlayerScore> scores = new ArrayList<>();
        for(Player player : players){
            if(player.score != lastScore || player.correctTime != lastTime){
                //since the list is sorted, all differences means a lower rank
                position++;
                lastScore = player.score;
                lastTime = player.correctTime;
            }
            scores.add(
                    new PlayerScore(
                        player.getName(),
                        position,
                        player.score,
                        player.score,
                        player.correctTime,
                        player.time
                    )
            );
        }
        return scores;
    }

    @Data
    private static class Player implements Comparable<Player>{
        private final String name;
        private int score = 0;
        private long time = 0L;
        private long correctTime = 0L;

        @Override
        public int compareTo(@NonNull Player o) {
            if(score != o.score){
                return score - o.score;
            }
            else if(correctTime > o.correctTime){
                return 1;
            }else if(correctTime < o.correctTime){
                return -1;
            }
            return 0;
        }
    }
}
