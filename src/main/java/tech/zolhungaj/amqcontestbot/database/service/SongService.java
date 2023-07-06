package tech.zolhungaj.amqcontestbot.database.service;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.zolhungaj.amqapi.servercommands.objects.SongInfo;
import tech.zolhungaj.amqcontestbot.database.enums.SongTypeEnum;
import tech.zolhungaj.amqcontestbot.database.model.SongEntity;
import tech.zolhungaj.amqcontestbot.database.repository.SongRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SongService {
    private final SongRepository repository;

    @Transactional
    public SongEntity getSongEntityFromSongInfo(@NonNull SongInfo songInfo){
        int animeId = songInfo.annId();
        SongTypeEnum type = switch (songInfo.type()){
            case OPENING -> SongTypeEnum.OPENING;
            case ENDING -> SongTypeEnum.ENDING;
            case INSERT -> SongTypeEnum.INSERT;
            default -> throw new IllegalArgumentException("Unexpected value: " + songInfo.type());
        };
        int number = songInfo.typeNumber();
        String title = songInfo.songName();
        String artist = songInfo.artist();
        Optional<SongEntity> optional = repository.findByAnimeIdIsAndTypeIsAndNumberIsAndTitleIsAndArtistIs(
                animeId,
                type,
                number,
                title,
                artist);
        if(optional.isPresent()){
            return optional.get();
        }else{
            SongEntity songEntity = new SongEntity();
            songEntity.setAnimeId(animeId);
            songEntity.setType(type);
            songEntity.setNumber(number);
            songEntity.setTitle(title);
            songEntity.setArtist(artist);
            return repository.save(songEntity);
        }
    }
}
