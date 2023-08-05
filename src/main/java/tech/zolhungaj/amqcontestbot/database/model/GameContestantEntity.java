package tech.zolhungaj.amqcontestbot.database.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NonNull;
import tech.zolhungaj.amqcontestbot.room.game.GameContestant;

@Data
@Entity
@Table(name = "game_contestant", schema = "public", catalog = "amq_contest_bot")
public class GameContestantEntity {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id")
    private int id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", referencedColumnName = "id", updatable = false)
    private GameEntity game;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contestant_id", referencedColumnName = "id", updatable = false)
    private ContestantEntity contestant;
    @Basic
    @Column(name = "position")
    private int position = -1;
    @Basic
    @Column(name = "score")
    private int score = 0;
    @Basic
    @Column(name = "game_mode_score")
    private int gameModeScore = 0;
    @Basic
    @Column(name = "correct_count")
    private int correctCount = 0;
    @Basic
    @Column(name = "miss_count")
    private int missCount = 0;
    @Basic
    @Column(name = "correct_time")
    private int correctTime = 0;
    @Basic
    @Column(name = "miss_time")
    private int missTime = 0;
    @Basic
    @Column(name = "deleted")
    private boolean deleted = false;

    public void updateFromGameContestant(@NonNull GameContestant gameContestant){
        this.position = gameContestant.getPosition();
        this.score = gameContestant.getScore();
        this.gameModeScore = gameContestant.getGameModeScore();
        this.correctCount = gameContestant.getCorrectCount();
        this.missCount = gameContestant.getMissCount();
        this.correctTime = (int) gameContestant.getCorrectTime();
        this.missTime = (int) gameContestant.getMissTime();
    }
}
