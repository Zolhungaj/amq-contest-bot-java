package tech.zolhungaj.amqcontestbot.room.game;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public final class GamePlayer extends AbstractGameEntity implements PlayerInformation {
    private final int gamePlayerId;
    private final String playerName;
    private boolean disconnected = false;

}
