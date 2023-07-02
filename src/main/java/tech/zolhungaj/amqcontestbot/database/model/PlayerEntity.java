package tech.zolhungaj.amqcontestbot.database.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tech.zolhungaj.amqapi.servercommands.objects.PlayerAvatar;

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
    private PlayerAvatar avatar;

    public Optional<Integer> getLevel(){
        return Optional.ofNullable(level);
    }

    public Optional<PlayerAvatar> getAvatar(){
        return Optional.ofNullable(avatar);
    }
}
