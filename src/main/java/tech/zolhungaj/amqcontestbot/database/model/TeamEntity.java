package tech.zolhungaj.amqcontestbot.database.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Data
@Entity
@Table(name = "team", schema = "public", catalog = "amq_contest_bot")
public class TeamEntity {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id")
    private int id;
    @Basic
    @Column(name = "name")
    private String name;

    @OneToOne(fetch = FetchType.LAZY, mappedBy = "team_id")
    @JoinColumn(name = "id")
    private ContestantEntity contestant;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "team_player_link",
            joinColumns = @JoinColumn(name = "team_id"),
            inverseJoinColumns = @JoinColumn(name = "player_id")
    )
    private List<PlayerEntity> players;
}
