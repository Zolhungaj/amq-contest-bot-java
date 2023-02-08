package tech.zolhungaj.amqcontestbot.gamemode;

import java.util.Collection;

public abstract non-sealed class AbstractGameMode implements GameMode{
    public final void start(Collection<String> players){
        this.reset();
        this.init(players);
    }

    protected abstract void reset();
    protected abstract void init(Collection<String> players);
}
