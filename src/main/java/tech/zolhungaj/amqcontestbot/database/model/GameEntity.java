package tech.zolhungaj.amqcontestbot.database.model;

import jakarta.persistence.*;
import lombok.Data;

import java.sql.Timestamp;
import java.util.List;
import java.util.Set;

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
    private Timestamp start;
    @Basic
    @Column(name = "finish")
    private Timestamp finish;

    @OneToMany(mappedBy = "game")
    private List<GameSongEntity> songs;

    @OneToMany(mappedBy = "game")
    private Set<GameContestantEntity> contestants;
}