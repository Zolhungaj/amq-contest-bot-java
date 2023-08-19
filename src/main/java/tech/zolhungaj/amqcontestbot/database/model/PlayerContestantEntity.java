package tech.zolhungaj.amqcontestbot.database.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Entity
@DiscriminatorValue("PLAYER")
public class PlayerContestantEntity extends ContestantEntity{
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "player_id")
    private PlayerEntity player;

    @Override
    public String getName() {
        return player != null ? player.getOriginalName() : null;
    }
}
