package tech.zolhungaj.amqcontestbot.database.model;

import jakarta.persistence.*;
import lombok.Data;
import tech.zolhungaj.amqcontestbot.database.enums.RulesetEnum;

import java.io.Serializable;
import java.time.OffsetDateTime;

@Data
@Entity
@Table(name = "leaderboard_count", schema = "public", catalog = "amq_contest_bot")
@IdClass(LeaderboardCountView.LeaderboardId.class)
public class LeaderboardCountView {
    @Id
    @Basic
    @Column(name = "contestant_id")
    private Integer contestantId;

    @Id
    @Basic
    @Column(name = "game_mode_score")
    private Integer gameModeScore;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "ruleset")
    private RulesetEnum ruleset;

    @Id
    @Basic
    @Column(name = "team_size")
    private Integer teamSize;


    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "contestant_id", referencedColumnName = "id", insertable = false, updatable = false)
    private ContestantEntity contestant;

    @Basic
    @Column(name = "times_achieved")
    private Long timesAchieved;
    @Basic
    @Column(name = "earliest_achieved")
    private OffsetDateTime earliestAchieved;

    @Data
    public static class LeaderboardId implements Serializable {
        private Integer contestantId;
        private Integer gameModeScore;
        private RulesetEnum ruleset;
        private Integer teamSize;
    }
}
