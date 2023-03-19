package tech.zolhungaj.amqcontestbot.room.game;

public interface PlayerInformation {
    int getGamePlayerId();
    String getPlayerName();
    void setDisconnected(boolean disconnected);
    void isDisconnected();
}
