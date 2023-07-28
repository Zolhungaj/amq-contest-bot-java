package tech.zolhungaj.amqcontestbot.database.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Immutable;

/**
 * Binds a scoring entity (player or team) to games and other tables.
 */
@Data
@Entity
@Table(name = "contestant", schema = "public", catalog = "amq_contest_bot")
@Immutable
@DiscriminatorColumn(name = "type")
public class ContestantEntity {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id")
    private int id;
}
