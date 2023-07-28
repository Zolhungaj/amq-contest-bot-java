package tech.zolhungaj.amqcontestbot.database.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "player", schema = "public", catalog = "amq_contest_bot")
@Data
public class PlayerEntity {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id")
    private int id;
    @Basic
    @Column(name = "original_name")
    private String originalName;
    @Column(name = "level")
    private Integer level;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "avatar")
    private PlayerAvatarEntity avatar;

    @OneToOne(fetch = FetchType.LAZY, mappedBy = "player_id")
    @JoinColumn(name = "id")
    private ContestantEntity contestant;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "team_player_link",
            joinColumns = @JoinColumn(name = "player_id"),
            inverseJoinColumns = @JoinColumn(name = "team_id")
    )
    private List<TeamEntity> teams;

    public Optional<Integer> getLevel(){
        return Optional.ofNullable(level);
    }

    public Optional<PlayerAvatarEntity> getAvatar(){
        return Optional.ofNullable(avatar);
    }
}
