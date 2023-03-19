package tech.zolhungaj.amqcontestbot.room;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public final class GameTeam extends AbstractGameEntity implements TeamIdentifier {
    private final int teamNumber;
    private final List<TeamPlayer> players;
}
