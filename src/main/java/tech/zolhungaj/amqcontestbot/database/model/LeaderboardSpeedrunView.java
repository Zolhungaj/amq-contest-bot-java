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
@Table(name = "leaderboard_speedrun", schema = "public", catalog = "amq_contest_bot")
@IdClass(LeaderboardSpeedrunView.LeaderboardId.class)
@Immutable
public class LeaderboardSpeedrunView implements LeaderboardView{
    @Id
    @Basic
    @Column(name = "contestant_id")
    private int contestantId;

    @Id
    @Basic
    @Column(name = "game_mode_score")
    private int gameModeScore;

    @Id
    @Basic
    @Column(name = "correct_time")
    private long correctTime;

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
        return "%d@%dms".formatted(gameModeScore, correctTime);
    }

    @Override
    public int compareTo(@NonNull LeaderboardView o) {
        if(o instanceof LeaderboardSpeedrunView lbv){
            return Comparator
                    .comparingLong(LeaderboardSpeedrunView::getGameModeScore)
                    .thenComparing(Comparator.comparingLong(LeaderboardSpeedrunView::getCorrectTime).reversed())
                    .thenComparing(Comparator.comparing(LeaderboardSpeedrunView::getEarliestAchieved).reversed())
                    .compare(this, lbv);
        }
        return 0;
    }

    @Data
    public static class LeaderboardId implements Serializable {
        private int contestantId;
        private int gameModeScore;
        private long correctTime;
        private RulesetEnum ruleset;
        private int teamSize;
    }
}
