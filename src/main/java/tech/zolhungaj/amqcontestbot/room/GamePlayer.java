package tech.zolhungaj.amqcontestbot.room;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public final class GamePlayer extends AbstractGameEntity implements PlayerIdentifier {
    private final int gamePlayerId;
    private final String playerName;
    private boolean isDisconnected = false;

}
