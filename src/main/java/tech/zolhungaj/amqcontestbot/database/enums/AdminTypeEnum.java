package tech.zolhungaj.amqcontestbot.database.enums;

public enum AdminTypeEnum {
    OWNER, //Person running the bot
    HOST, //The bot itself so it cannot be unmodded
    ADMIN, //Powerful moderators
    MODERATOR //regular moderators
}
