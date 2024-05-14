package tech.zolhungaj.amqcontestbot.chat;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tech.zolhungaj.amqapi.servercommands.gameroom.game.AnswerResults;
import tech.zolhungaj.amqapi.servercommands.gameroom.lobby.PlayerChangedToSpectator;
import tech.zolhungaj.amqapi.servercommands.objects.PlayerAvatar;
import tech.zolhungaj.amqapi.servercommands.objects.SongInfo;
import tech.zolhungaj.amqcontestbot.ApiManager;
import tech.zolhungaj.amqcontestbot.commands.ChatCommands;
import tech.zolhungaj.amqcontestbot.commands.Grant;
import tech.zolhungaj.amqcontestbot.database.model.PlayerAvatarEntity;
import tech.zolhungaj.amqcontestbot.database.model.PlayerEntity;
import tech.zolhungaj.amqcontestbot.exceptions.IncorrectArgumentCountException;
import tech.zolhungaj.amqcontestbot.exceptions.IncorrectCommandUsageException;

import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;

/** More of a for-fun module, occasionally comments on songs and player achievements.
 */
@Component
@RequiredArgsConstructor
public class QuipGenerator {
    private double chattiness = 0.25;
    private final RandomGenerator randomGenerator = RandomGenerator.of("L64X128MixRandom");
    private final ChatController chatController;
    private final ChatCommands chatCommands;
    private final ApiManager api;

    @PostConstruct
    private void init(){
        chatCommands.register((sender, arguments) -> {
            if(arguments.size() != 1){
                throw new IncorrectArgumentCountException(1);
            }
            try{
                chattiness = Double.parseDouble(arguments.getFirst());
            }catch (NumberFormatException e){
                throw new IncorrectCommandUsageException("quip.setchattiness.invalid-number");
            }
        }, Grant.MODERATOR, "setchattiness");
        api.on(PlayerChangedToSpectator.class, spectator -> commentOnPlayerToSpectator(spectator.spectatorDescription().playerName()));
        api.on(AnswerResults.class, this::quipAboutAnime);
    }

    private void commentOnPlayerToSpectator(String nickname){
        if(shouldTrigger(8)){
            chatController.send("quip.player-to-spectator", nickname);
        }
    }

    private void quipAboutAnime(AnswerResults answerResults){
        if(shouldTrigger()){
            Optional<SongInfo> songInfo = Optional.ofNullable(answerResults).map(AnswerResults::songInfo);
            List<String> names = songInfo.map(SongInfo::alternativeAnimeNames).orElse(List.of());
            /*
            * Table of probabilities:
            * anime name: 50% (1/2)
            * anime genre: 16.66% (1/3 /2)
            * anime tag: 16.66% (1/3 /2)
            * vintage: 16.66% (1/3 /2)
            * In the event that one is absent its probability is redistributed to the others after it on the list.
            * If all are absent a generic remark is made instead.
            * */
            List<String> genres = songInfo.map(SongInfo::animeGenre).orElse(List.of());
            List<String> tags = songInfo.map(SongInfo::animeTags).orElse(List.of());
            Optional<String> vintage = songInfo.map(SongInfo::vintage);
            if(!names.isEmpty() && randomGenerator.nextInt(2) == 0){
                chatController.send("quip.anime.name", names.get(randomGenerator.nextInt(names.size())));
            }else if(!genres.isEmpty() && randomGenerator.nextInt(3) == 0){
                chatController.send("quip.anime.genre", genres.get(randomGenerator.nextInt(genres.size())));
            }else if(!tags.isEmpty() && randomGenerator.nextInt(2) == 0){
                chatController.send("quip.anime.tag", tags.get(randomGenerator.nextInt(tags.size())));
            }else if(vintage.isPresent()){
                chatController.send("quip.anime.vintage", vintage.get());
            }else{
                chatController.send("quip.generic");
            }
        }
    }

    public void commentOnPlayer(PlayerEntity playerEntity, String nickname, int currentLevel, PlayerAvatar avatar, boolean wasInRoom){
        if(commentOnMilestone(playerEntity, nickname, currentLevel, wasInRoom) || commentOnAvatar(playerEntity, nickname, avatar, wasInRoom)){
            return;
        }
        if(wasInRoom){
            if(shouldTrigger(16)){
                chatController.send("quip.spectator-to-player", nickname);
            }
        }else{
            chatController.send("welcome.returning.player", nickname);
        }
    }

    private boolean commentOnMilestone(PlayerEntity playerEntity, String nickname, int currentLevel, boolean wasInRoom){
        Optional<Integer> previousLevelOptional = playerEntity.getLevel();
        if(previousLevelOptional.isEmpty() || previousLevelOptional.get() >= currentLevel){
            return false;
        }
        int previousLevel = previousLevelOptional.get();
        int firstLevelPassedThrough = previousLevel + 1;
        //check all levels passed through to decide what milestone to congratulate on
        int thousands = 0;
        int hundreds = 0;
        int fifties = 0;
        int tens = 0;
        int difference = currentLevel - previousLevel;
        for(int i = firstLevelPassedThrough; i < currentLevel; i++){
            if(i % 1000 == 0) {
                thousands = i;
            }
            if(i % 100 == 0){
                hundreds = i;
            }
            if(i % 50 == 0) {
                fifties = i;
            }
            if(i % 10 == 0) {
                tens = i;
            }
        }
        final String postfix;
        if(wasInRoom){
            postfix = ".in-room";
        }else{
            postfix = ".on-join";
        }
        if(thousands > 0){
            chatController.send("welcome.milestone.thousands" + postfix, nickname, thousands);
        }else if (hundreds > 0){
            chatController.send("welcome.milestone.hundreds" + postfix, nickname, hundreds);
        }else if (fifties > 0){
            chatController.send("welcome.milestone.fifties" + postfix, nickname, fifties);
        }else if (tens > 0){
            chatController.send("welcome.milestone.tens" + postfix, nickname, tens);
        }else if (difference > 1){
            chatController.send("welcome.milestone.multiple" + postfix, nickname, difference);
        }else{
            chatController.send("welcome.milestone.single" + postfix, nickname);
        }
        return true;
    }

    private boolean commentOnAvatar(PlayerEntity playerEntity, String nickname, PlayerAvatar currentAvatarRaw, boolean wasInRoom){
        PlayerAvatarEntity currentAvatar = PlayerAvatarEntity.of(currentAvatarRaw);
        Optional<PlayerAvatarEntity> previousAvatarOptional = playerEntity.getAvatar();
        if(previousAvatarOptional.isEmpty() || previousAvatarOptional.get().equals(currentAvatar)){
            return false;
        }
        final String postfix;
        final int divider;
        if(wasInRoom){
            postfix = ".in-room";
            divider = 16;
        }else{
            postfix = ".on-join";
            divider = 8;
        }
        if(shouldTrigger(divider)){
            chatController.send("quip.player.avatar" + postfix, nickname);
            return true;
        }
        return false;
    }

    private boolean shouldTrigger(){
        return shouldTrigger(1);
    }

    private boolean shouldTrigger(int divider){
        return randomGenerator.nextDouble() >= (1.0 - chattiness/divider);
    }

}
