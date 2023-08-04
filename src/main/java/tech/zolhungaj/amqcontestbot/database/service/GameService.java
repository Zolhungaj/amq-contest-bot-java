package tech.zolhungaj.amqcontestbot.database.service;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tech.zolhungaj.amqcontestbot.database.enums.RulesetEnum;
import tech.zolhungaj.amqcontestbot.database.enums.ScoringTypeEnum;
import tech.zolhungaj.amqcontestbot.database.model.GameEntity;
import tech.zolhungaj.amqcontestbot.database.model.GameModeEntity;
import tech.zolhungaj.amqcontestbot.database.model.GameSongEntity;
import tech.zolhungaj.amqcontestbot.database.model.SongEntity;
import tech.zolhungaj.amqcontestbot.database.repository.GameModeRepository;
import tech.zolhungaj.amqcontestbot.database.repository.GameRepository;

import java.time.OffsetDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GameService {
    private final GameModeRepository gameModeRepository;
    private final GameRepository gameRepository;

    public GameEntity startGame(RulesetEnum ruleset, ScoringTypeEnum scoringTypeEnum, int teamSize){
        GameModeEntity gameModeEntity = getOrCreateGameMode(ruleset, scoringTypeEnum, teamSize);
        GameEntity gameEntity = new GameEntity();
        gameEntity.setStart(OffsetDateTime.now());
        gameEntity.setGameMode(gameModeEntity);
        return gameRepository.save(gameEntity);
    }

    public void finishGame(@NonNull GameEntity gameEntity){
        gameEntity.setFinish(OffsetDateTime.now());
        gameRepository.save(gameEntity);
    }

    public GameSongEntity createGameSong(@NonNull GameEntity game, @NonNull SongEntity song){
        GameSongEntity gameSongEntity = new GameSongEntity();
        gameSongEntity.setGame(game);
        gameSongEntity.setSong(song);
        int index = game.getSongs().size();
        gameSongEntity.setOrdinal(index);
        game.getSongs().add(gameSongEntity);
        return gameRepository.save(game).getSongs().get(index);
    }

    private GameModeEntity getOrCreateGameMode(RulesetEnum ruleset, ScoringTypeEnum scoringTypeEnum, int teamSize){
        Optional<GameModeEntity> optional = gameModeRepository.findByRulesetAndScoringTypeAndTeamSize(ruleset, scoringTypeEnum, teamSize);
        if(optional.isPresent()){
            return optional.get();
        }
        GameModeEntity gameModeEntity = new GameModeEntity();
        gameModeEntity.setRuleset(ruleset);
        gameModeEntity.setScoringType(scoringTypeEnum);
        gameModeEntity.setTeamSize(teamSize);
        gameModeEntity.setGameModeName(ruleset.name() + " " + scoringTypeEnum.name() + " " + teamSize);
        return gameModeRepository.save(gameModeEntity);
    }
}
