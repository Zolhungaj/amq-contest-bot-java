package tech.zolhungaj.amqcontestbot.room.lobby;

import lombok.With;

import java.util.Optional;

@With
public record LobbyPlayer(
        String playerName,
        int gamePlayerId,
        boolean isReady,
        boolean inLobby,
        Optional<Integer> teamNumber,
        int consecutiveGames
) {}
