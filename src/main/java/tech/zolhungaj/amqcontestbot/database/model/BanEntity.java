package tech.zolhungaj.amqcontestbot.database.model;

import jakarta.persistence.*;
import lombok.Data;

import java.sql.Timestamp;

@Entity
@Table(name = "ban", schema = "public", catalog = "amq_contest_bot")
@Data
public class BanEntity {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id")
    private int id;
    @Basic
    @Column(name = "player_id")
    private int playerId;
    @Basic
    @Column(name = "start")
    private Timestamp start;
    @Basic
    @Column(name = "expiry")
    private Timestamp expiry;
    @Basic
    @Column(name = "reason")
    private String reason;
}
