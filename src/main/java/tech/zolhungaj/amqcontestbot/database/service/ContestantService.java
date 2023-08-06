package tech.zolhungaj.amqcontestbot.database.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tech.zolhungaj.amqcontestbot.database.model.*;
import tech.zolhungaj.amqcontestbot.database.repository.PlayerContestantRepository;
import tech.zolhungaj.amqcontestbot.database.repository.TeamContestantRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ContestantService {
    private final TeamContestantRepository teamRepository;
    private final PlayerContestantRepository playerRepository;

    public TeamContestantEntity getOrCreateContestant(TeamEntity team){
        Optional<TeamContestantEntity> optional = teamRepository.findByTeam(team);
        if(optional.isPresent()) {
            return optional.get();
        }else{
            TeamContestantEntity teamContestantEntity = new TeamContestantEntity();
            teamContestantEntity.setTeam(team);
            return teamRepository.save(teamContestantEntity);
        }
    }

    public PlayerContestantEntity getOrCreateContestant(PlayerEntity player){
        Optional<PlayerContestantEntity> optional = playerRepository.findByPlayer(player);
        if(optional.isPresent()) {
            return optional.get();
        }else{
            PlayerContestantEntity playerContestantEntity = new PlayerContestantEntity();
            playerContestantEntity.setPlayer(player);
            return playerRepository.save(playerContestantEntity);
        }
    }
}
