package tech.zolhungaj.amqcontestbot.database.service;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.zolhungaj.amqapi.servercommands.objects.SongInfo;
import tech.zolhungaj.amqcontestbot.database.enums.BroadcastFormatEnum;
import tech.zolhungaj.amqcontestbot.database.enums.SeasonEnum;
import tech.zolhungaj.amqcontestbot.database.enums.SongTypeEnum;
import tech.zolhungaj.amqcontestbot.database.model.AnimeEntity;
import tech.zolhungaj.amqcontestbot.database.model.SongEntity;
import tech.zolhungaj.amqcontestbot.database.repository.AnimeRepository;
import tech.zolhungaj.amqcontestbot.database.repository.SongRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SongService {
    private final SongRepository repository;
    private final AnimeRepository animeRepository;

    @Transactional
    public SongEntity updateAndGetSongEntityFromSongInfo(@NonNull SongInfo songInfo){
        AnimeEntity animeEntity = updateAnimeWithSongInfo(songInfo);
        log.info("AnimeEntity: {}", animeEntity);
        SongTypeEnum type = switch (songInfo.type()){
            case OPENING -> SongTypeEnum.OPENING;
            case ENDING -> SongTypeEnum.ENDING;
            case INSERT -> SongTypeEnum.INSERT;
            default -> throw new IllegalArgumentException("Unexpected value: " + songInfo.type());
        };
        int number = songInfo.typeNumber();
        String title = songInfo.songName();
        String artist = songInfo.artist();
        Optional<SongEntity> optional = repository.findByAnimeIsAndTypeIsAndNumberIsAndTitleIsAndArtistIs(
                animeEntity,
                type,
                number,
                title,
                artist);
        final SongEntity songEntity;
        if(optional.isPresent()){
            songEntity = optional.get();
        }else{
            songEntity = new SongEntity();
            songEntity.setAnime(animeEntity);
            songEntity.setType(type);
            songEntity.setNumber(number);
            songEntity.setTitle(title);
            songEntity.setArtist(artist);
        }
        songEntity.setDifficulty(songInfo.animeDifficulty().doubleValue());
        log.info("Saving song: {}", songEntity);
        repository.save(songEntity);
        return songEntity;
    }

    private AnimeEntity updateAnimeWithSongInfo(SongInfo songInfo){
        int animeId = songInfo.annId();
        Optional<AnimeEntity> optional = animeRepository.findById(animeId);
        final AnimeEntity entity;
        if(optional.isPresent()){
            entity = optional.get();
        }else{
            entity = new AnimeEntity();
            entity.setId(animeId);
        }
        Optional.ofNullable(songInfo.animeScore())
                .map(doubleValue -> BigDecimal.valueOf(doubleValue).setScale(2, RoundingMode.HALF_DOWN))
                .ifPresentOrElse(entity::setRating, () -> entity.setRating(null));
        Optional.ofNullable(songInfo.mainAnimeNames().english()).ifPresentOrElse(entity::setEnglishName, () -> entity.setEnglishName(null));
        Optional.ofNullable(songInfo.mainAnimeNames().romaji()).ifPresentOrElse(entity::setRomajiName, () -> entity.setRomajiName(null));
        BroadcastFormatEnum broadcastFormat = switch (songInfo.animeType().toLowerCase(Locale.US)){
            case "tv" -> BroadcastFormatEnum.TV;
            case "ona" -> BroadcastFormatEnum.ONA;
            case "ova" -> BroadcastFormatEnum.OVA;
            case "movie" -> BroadcastFormatEnum.MOVIE;
            case "special" -> BroadcastFormatEnum.SPECIAL;
            default -> throw new IllegalArgumentException("Unexpected value: " + songInfo.animeType());
        };
        entity.setBroadcastFormat(broadcastFormat);
        Optional.ofNullable(songInfo.siteIds().kitsuId()).ifPresentOrElse(entity::setKitsuId, () -> entity.setKitsuId(null));
        Optional.ofNullable(songInfo.siteIds().animeNewsNetworkId()).ifPresentOrElse(entity::setAnimenewsnetworkId, () -> entity.setAnimenewsnetworkId(null));
        Optional.ofNullable(songInfo.siteIds().myAnimeListId()).ifPresentOrElse(entity::setMyanimelistId, () -> entity.setMyanimelistId(null));
        Optional.ofNullable(songInfo.siteIds().aniListId()).ifPresentOrElse(entity::setAnilistId, () -> entity.setAnilistId(null));
        SeasonEnum seasonEnum = switch (songInfo.vintage().key()){
            case "song_library.anime_entry.vintage.spring" -> SeasonEnum.SPRING;
            case "song_library.anime_entry.vintage.summer" -> SeasonEnum.SUMMER;
            case "song_library.anime_entry.vintage.fall" -> SeasonEnum.AUTUMN;
            case "song_library.anime_entry.vintage.winter" -> SeasonEnum.WINTER;
            default -> throw new IllegalArgumentException("Unexpected value: " + songInfo.vintage().key());
        };
        int year = songInfo.vintage().data().getYear();
        entity.setYear(year);
        entity.setSeason(seasonEnum);
        return animeRepository.save(entity);
    }
}
