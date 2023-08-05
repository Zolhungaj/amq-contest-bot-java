package tech.zolhungaj.amqcontestbot.database.service;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tech.zolhungaj.amqcontestbot.database.model.PlayerEntity;
import tech.zolhungaj.amqcontestbot.database.model.TeamEntity;
import tech.zolhungaj.amqcontestbot.database.repository.PlayerRepository;
import tech.zolhungaj.amqcontestbot.database.repository.TeamRepository;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TeamService {
    private final TeamRepository repository;

    public TeamEntity getOrCreateTeam(@NonNull Collection<PlayerEntity> players){
        Set<PlayerEntity> playerSet = Set.copyOf(players);
        Optional<TeamEntity> team = repository.findByPlayersEquals(playerSet);
        if(team.isPresent()){
            return team.get();
        }else{
            TeamEntity teamEntity = new TeamEntity();
            teamEntity.setPlayers(playerSet);
            teamEntity.setName("Team " + String.join(",", playerSet.stream().map(PlayerEntity::getOriginalName).sorted().toList()));
            return repository.save(teamEntity);
        }
    }


    public void save(TeamEntity team){
        repository.save(team);
    }
}
