package tech.zolhungaj.amqcontestbot.database.model;

import jakarta.persistence.*;
import lombok.Data;
import tech.zolhungaj.amqcontestbot.database.enums.RulesetEnum;
import tech.zolhungaj.amqcontestbot.database.enums.ScoringTypeEnum;

@Data
@Entity
@Table(name = "game_mode", schema = "public", catalog = "amq_contest_bot")
public class GameModeEntity {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id")
    private int id;
    @Basic
    @Column(name = "name")
    private String gameModeName;
    @Enumerated(EnumType.STRING)
    @Column(name = "ruleset")
    private RulesetEnum ruleset;
    @Enumerated(EnumType.STRING)
    @Column(name = "scoring_type")
    private ScoringTypeEnum scoringType;
    @Basic
    @Column(name = "team_size")
    private int teamSize;
}
