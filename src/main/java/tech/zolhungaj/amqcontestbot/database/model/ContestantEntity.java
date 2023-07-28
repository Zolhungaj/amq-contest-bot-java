package tech.zolhungaj.amqcontestbot.database.model;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Binds a scoring entity (player or team) to games and other tables.
 */
@Data
@Entity
@Table(name = "contestant", schema = "public", catalog = "amq_contest_bot")
@DiscriminatorColumn(name = "type")
public class ContestantEntity {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id")
    private int id;
}
