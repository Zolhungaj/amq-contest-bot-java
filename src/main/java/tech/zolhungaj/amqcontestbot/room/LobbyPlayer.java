package tech.zolhungaj.amqcontestbot.room;

import lombok.Getter;
import lombok.With;

import java.util.Optional;

@With
public record LobbyPlayer(
        @Getter
        String playerName,
        @Getter
        int gamePlayerId,
        boolean isReady,
        boolean inLobby,
        Optional<Integer> teamNumber,
        int consecutiveGames
) implements PlayerIdentifier{}
