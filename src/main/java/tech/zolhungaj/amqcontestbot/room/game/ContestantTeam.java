package tech.zolhungaj.amqcontestbot.room.game;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public final class ContestantTeam extends AbstractGameContestant implements TeamIdentifier {
    private final int teamNumber;
    private final List<TeamPlayer> players;
}
