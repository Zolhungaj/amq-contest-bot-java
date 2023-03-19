package tech.zolhungaj.amqcontestbot.room.game;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public final class TeamPlayer implements PlayerInformation, TeamIdentifier{
    private final int gamePlayerId;
    private final String playerName;
    private boolean disconnected = false;
    private final int teamNumber;
}
