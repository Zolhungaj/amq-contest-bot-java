package tech.zolhungaj.amqcontestbot.gamemode;

import java.util.*;

public abstract non-sealed class AbstractGameMode implements GameMode{
    public final void start(Collection<String> players){
        this.reset();
        this.init(players);
    }

    private final Map<String, PlayerScore> playerMap = new HashMap<>();

    private void reset(){
        playerMap.clear();
    }

    private void init(Collection<String> players){
        players.forEach(player -> playerMap.put(player, new PlayerScore(player, -1, 0, 0, 0, 0)));
    }

    protected abstract Comparator<PlayerScore> comparator();

    @Override
    public final Collection<GameMode.PlayerScore> finish() {
        List<PlayerScore> players =  playerMap.values()
                .stream()
                .sorted(Collections.reverseOrder(this.comparator()))
                .toList();
        int position = 0;
        List<PlayerScore> scores = new ArrayList<>();
        PlayerScore previous = null;
        for(PlayerScore player : players){
            if(previous == null || this.comparator().compare(previous, player) != 0){
                //since the list is sorted, all differences means a lower rank
                position++;
            }
            previous = player;
            scores.add(
                    player.withPosition(position)
            );
        }
        return scores;
    }
}
