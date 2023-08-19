package tech.zolhungaj.amqcontestbot.database.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NonNull;
import org.hibernate.annotations.Immutable;
import tech.zolhungaj.amqcontestbot.database.enums.RulesetEnum;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Comparator;

@Data
@Entity
@Table(name = "leaderboard_count", schema = "public", catalog = "amq_contest_bot")
@IdClass(LeaderboardCountView.LeaderboardId.class)
@Immutable
public class LeaderboardCountView implements LeaderboardView{
    @Id
    @Basic
    @Column(name = "contestant_id")
    private int contestantId;

    @Id
    @Basic
    @Column(name = "game_mode_score")
    private int gameModeScore;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "ruleset")
    private RulesetEnum ruleset;

    @Id
    @Basic
    @Column(name = "team_size")
    private int teamSize;


    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "contestant_id", referencedColumnName = "id", insertable = false, updatable = false)
    private ContestantEntity contestant;

    @Basic
    @Column(name = "times_achieved")
    private int timesAchieved;
    @Basic
    @Column(name = "earliest_achieved")
    private OffsetDateTime earliestAchieved;

    @Override
    public String getName(){
        return contestant.getName();
    }

    @Override
    public String getScoreRepresentation() {
        return String.valueOf(gameModeScore);
    }

    @Override
    public int compareTo(@NonNull LeaderboardView o) {
        if(o instanceof LeaderboardCountView lbc){
            return Comparator
                    .comparingInt(LeaderboardCountView::getGameModeScore)
                    .thenComparing(Comparator.comparing(LeaderboardCountView::getEarliestAchieved).reversed())
                    .compare(this, lbc);
        }
        return 0;
    }

    @Data
    public static class LeaderboardId implements Serializable {
        private int contestantId;
        private int gameModeScore;
        private RulesetEnum ruleset;
        private int teamSize;
    }
}
