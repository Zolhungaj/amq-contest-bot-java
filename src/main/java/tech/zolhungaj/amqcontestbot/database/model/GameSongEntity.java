package tech.zolhungaj.amqcontestbot.database.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "game_song", schema = "public", catalog = "amq_contest_bot")
public class GameSongEntity {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id")
    private int id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", referencedColumnName = "id", insertable = false, updatable = false)
    private GameEntity game;
    @Basic
    @Column(name = "ordinal")
    private int ordinal;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "song_id", referencedColumnName = "id")
    private SongEntity song;
}
