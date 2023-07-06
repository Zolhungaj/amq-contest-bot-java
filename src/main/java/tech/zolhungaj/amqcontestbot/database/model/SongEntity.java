package tech.zolhungaj.amqcontestbot.database.model;

import jakarta.persistence.*;
import lombok.Data;
import tech.zolhungaj.amqcontestbot.database.enums.SongTypeEnum;

@Entity
@Table(name = "song", schema = "public", catalog = "amq_contest_bot")
@Data
public class SongEntity {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id")
    private int id;
    @Basic
    @Column(name = "anime_id")
    private int animeId;
    @Basic
    @Column(name = "type")
    private SongTypeEnum type;
    @Basic
    @Column(name = "number")
    private int number;
    @Basic
    @Column(name = "title")
    private String title;
    @Basic
    @Column(name = "artist")
    private String artist;
    @Basic
    @Column(name = "difficulty")
    private Double difficulty;
}
