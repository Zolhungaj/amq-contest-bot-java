package tech.zolhungaj.amqcontestbot.database.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "admin_log", schema = "public", catalog = "amq_contest_bot")
@Data
public class AdminLogEntity {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id")
    private int id;
    @Basic
    @Column(name = "admin_id")
    private int adminId;
    @Basic
    @Column(name = "action")
    private String action;
}
