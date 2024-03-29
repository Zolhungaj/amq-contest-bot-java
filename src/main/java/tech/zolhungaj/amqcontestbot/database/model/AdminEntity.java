package tech.zolhungaj.amqcontestbot.database.model;

import jakarta.persistence.*;
import lombok.Data;
import tech.zolhungaj.amqcontestbot.database.enums.AdminTypeEnum;

@Entity
@Table(name = "admin", schema = "public", catalog = "amq_contest_bot")
@Data
public class AdminEntity {
    @Id
    @Column(name = "player_id")
    private int playerId;
    @Enumerated(EnumType.STRING)
    @Column(name = "admin_type")
    private AdminTypeEnum adminType;
}
