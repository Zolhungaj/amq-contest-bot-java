package tech.zolhungaj.amqcontestbot.database.model;

import jakarta.persistence.*;
import lombok.Data;
import tech.zolhungaj.amqcontestbot.database.enums.BroadcastFormatEnum;
import tech.zolhungaj.amqcontestbot.database.enums.SeasonEnum;

import java.math.BigDecimal;

@Data
@Entity
@Table(name = "anime", schema = "public", catalog = "amq_contest_bot")
public class AnimeEntity {
    @Id
    @Column(name = "id")
    private int id;
    @Basic
    @Column(name = "name_english")
    private String englishName;
    @Basic
    @Column(name = "name_romaji")
    private String romajiName;
    @Enumerated(EnumType.STRING)
    @Column(name = "broadcast_format")
    private BroadcastFormatEnum broadcastFormat;
    @Basic
    @Column(name = "kitsu_id")
    private Integer kitsuId;
    @Basic
    @Column(name = "animenewsnetwork_id")
    private Integer animenewsnetworkId;
    @Basic
    @Column(name = "myanimelist_id")
    private Integer myanimelistId;
    @Basic
    @Column(name = "anilist_id")
    private Integer anilistId;
    @Basic
    @Column(name = "rating")
    private BigDecimal rating;
    @Basic
    @Column(name = "year")
    private Integer year;
    @Enumerated(EnumType.STRING)
    @Column(name = "season")
    private SeasonEnum season;
}
