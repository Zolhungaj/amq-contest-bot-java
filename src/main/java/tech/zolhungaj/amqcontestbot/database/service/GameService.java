package tech.zolhungaj.amqcontestbot.database.service;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tech.zolhungaj.amqcontestbot.database.enums.RulesetEnum;
import tech.zolhungaj.amqcontestbot.database.enums.ScoringTypeEnum;
import tech.zolhungaj.amqcontestbot.database.model.*;
import tech.zolhungaj.amqcontestbot.database.repository.*;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GameService {
    private final GameModeRepository modeRepository;
    private final GameRepository gameRepository;
    private final GameContestantRepository contestantRepository;
    private final GameSongRepository songRepository;
    private final ContestantSongAnswerRepository answerRepository;

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

    public GameContestantEntity createGameContestant(@NonNull GameEntity game, @NonNull ContestantEntity contestant){
        GameContestantEntity gameContestantEntity = new GameContestantEntity();
        gameContestantEntity.setGame(game);
        gameContestantEntity.setContestant(contestant);
        return contestantRepository.save(gameContestantEntity);
    }

    public GameSongEntity createGameSong(@NonNull GameEntity game, @NonNull SongEntity song){
        GameSongEntity gameSongEntity = new GameSongEntity();
        gameSongEntity.setGame(game);
        gameSongEntity.setSong(song);

        int maxOrdinal = songRepository.findFirstByGameOrderByOrdinalDesc(game)
                .map(GameSongEntity::getOrdinal)
                .filter(integer -> integer >= 0) //in case of database corruption, intentional or otherwise
                .orElse(-1); //songs are indexed from 0
        gameSongEntity.setOrdinal(maxOrdinal + 1);
        return songRepository.save(gameSongEntity);
    }

    private GameModeEntity getOrCreateGameMode(RulesetEnum ruleset, ScoringTypeEnum scoringTypeEnum, int teamSize){
        Optional<GameModeEntity> optional = modeRepository.findByRulesetAndScoringTypeAndTeamSize(ruleset, scoringTypeEnum, teamSize);
        if(optional.isPresent()){
            return optional.get();
        }
        GameModeEntity gameModeEntity = new GameModeEntity();
        gameModeEntity.setRuleset(ruleset);
        gameModeEntity.setScoringType(scoringTypeEnum);
        gameModeEntity.setTeamSize(teamSize);
        gameModeEntity.setGameModeName(ruleset.name() + " " + scoringTypeEnum.name() + " " + teamSize);
        return modeRepository.save(gameModeEntity);
    }

    public void updateGameContestants(Collection<GameContestantEntity> contestants) {
        contestantRepository.saveAll(contestants);
    }

    public void createGameAnswer(GameSongEntity gameSong, ContestantEntity contestant, boolean correct, String s, Duration playerAnswerTime) {
        ContestantSongAnswerEntity gameAnswerEntity = new ContestantSongAnswerEntity();
        gameAnswerEntity.setGameSong(gameSong);
        gameAnswerEntity.setContestant(contestant);
        gameAnswerEntity.setCorrect(correct);
        gameAnswerEntity.setAnswer(s);
        gameAnswerEntity.setAnswerTime(playerAnswerTime == null ? null : playerAnswerTime.toMillis());
        answerRepository.save(gameAnswerEntity);
    }
}
