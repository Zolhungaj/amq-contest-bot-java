package tech.zolhungaj.amqcontestbot.database.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Entity
@DiscriminatorValue("TEAM")
public class TeamContestantEntity extends ContestantEntity{
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "team_id")
    private TeamEntity team;

    @Override
    public String getName() {
        return team != null ? team.getName() : null;
    }
}
