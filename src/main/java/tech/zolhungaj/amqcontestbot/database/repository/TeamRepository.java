package tech.zolhungaj.amqcontestbot.database.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import tech.zolhungaj.amqcontestbot.database.model.PlayerEntity;
import tech.zolhungaj.amqcontestbot.database.model.TeamEntity;

import java.util.List;
import java.util.Set;

@Repository
public interface TeamRepository extends JpaRepository<TeamEntity, Integer> {
    @Query("""
            SELECT t FROM TeamEntity t
            inner join t.players p1
            where 1=1
                and p1 in :players
                and not exists(
                    select t2 from TeamEntity t2
                    inner join t2.players p2
                    where 1=1
                        and p2 not in :players
                        and t2.id = t.id
                )
            group by t
            having count(p1) = :size
            """)
    //finds all teams that are subsets of the given set of players, and verifies that the size is the same
    //if set A is a subset of set B, and size(A) = size(B), then A = B
    List<TeamEntity> findMatchingTeam(Set<PlayerEntity> players, int size);
}
