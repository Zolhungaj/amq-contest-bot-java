package tech.zolhungaj.amqcontestbot.database.model;

import jakarta.annotation.Nullable;
import tech.zolhungaj.amqapi.servercommands.objects.PlayerAvatar;

public record PlayerAvatarEntity(
        AvatarDescription avatar,
        BackgroundDescription background
) {
    public record AvatarDescription(
            int avatarId,
            int colorId,
            int characterId,
            String avatarName,
            String outfitName,
            String colorName,
            String backgroundFilename,
            Boolean colorActive,
            @Nullable String editor,
            int sizeModifier,
            String optionName,
            Boolean optionActive,
            Boolean active
    ) {
    }

    public record BackgroundDescription(
            int avatarId,
            int colorId,
            String avatarName,
            String outfitName,
            String backgroundHorizontal,
            String backgroundVertical
    ) {
    }


    public static PlayerAvatarEntity of(PlayerAvatar playerAvatar){
        return new PlayerAvatarEntity(
                new AvatarDescription(
                        playerAvatar.avatar().avatarId(),
                        playerAvatar.avatar().colorId(),
                        playerAvatar.avatar().characterId(),
                        playerAvatar.avatar().avatarName(),
                        playerAvatar.avatar().outfitName(),
                        playerAvatar.avatar().colorName(),
                        playerAvatar.avatar().backgroundFilename(),
                        playerAvatar.avatar().colorActive(),
                        playerAvatar.avatar().editor().orElse(null),
                        playerAvatar.avatar().sizeModifier(),
                        playerAvatar.avatar().optionName(),
                        playerAvatar.avatar().optionActive(),
                        playerAvatar.avatar().active()
                ),
                new BackgroundDescription(
                        playerAvatar.background().avatarId(),
                        playerAvatar.background().colorId(),
                        playerAvatar.background().avatarName(),
                        playerAvatar.background().outfitName(),
                        playerAvatar.background().backgroundHorizontal(),
                        playerAvatar.background().backgroundVertical()
                )
        );
    }
}