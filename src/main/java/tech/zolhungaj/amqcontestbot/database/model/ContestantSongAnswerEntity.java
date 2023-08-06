package tech.zolhungaj.amqcontestbot.database.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "contestant_song_answer", schema = "public", catalog = "amq_contest_bot")
public class ContestantSongAnswerEntity {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id")
    private int id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_song_id", referencedColumnName = "id")
    private GameSongEntity gameSong;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contestant_id", referencedColumnName = "id")
    private ContestantEntity contestant;
    @Basic
    @Column(name = "answer")
    private String answer;
    @Basic
    @Column(name = "correct")
    private boolean correct;
    @Basic
    @Column(name = "answer_time")
    private Long answerTime;
}
