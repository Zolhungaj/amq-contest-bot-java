package tech.zolhungaj.amqcontestbot.database.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "game", schema = "public", catalog = "amq_contest_bot")
public class GameEntity {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id")
    private int id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_mode", referencedColumnName = "id")
    private GameModeEntity gameMode;
    @Basic
    @Column(name = "start")
    private OffsetDateTime start;
    @Basic
    @Column(name = "finish")
    private OffsetDateTime finish;

    @OneToMany(mappedBy = "game")
    private List<GameSongEntity> songs;

    @OneToMany(mappedBy = "game")
    private List<GameContestantEntity> contestants;
}
