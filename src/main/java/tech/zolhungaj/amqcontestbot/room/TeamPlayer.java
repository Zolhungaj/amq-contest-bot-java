package tech.zolhungaj.amqcontestbot.room;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public final class TeamPlayer implements PlayerIdentifier, TeamIdentifier{
    private final int gamePlayerId;
    private final String playerName;
    private final int teamNumber;
    private boolean isDisconnected = false;
}
