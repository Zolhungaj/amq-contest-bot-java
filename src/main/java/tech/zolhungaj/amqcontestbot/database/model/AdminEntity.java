package tech.zolhungaj.amqcontestbot.database.model;

import jakarta.persistence.*;
import lombok.Data;
import tech.zolhungaj.amqcontestbot.database.enums.AdminType;

import java.util.Objects;

@Entity
@Table(name = "admin", schema = "public", catalog = "amq_contest_bot")
@Data
public class AdminEntity {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "player_id")
    private int playerId;
    @Basic
    @Column(name = "admin_type")
    private AdminType adminType;
}
