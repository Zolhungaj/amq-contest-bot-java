package tech.zolhungaj.amqcontestbot.database.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "internationalisation", schema = "public", catalog = "amq_contest_bot")
public class InternationalisationEntity {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id")
    private int id;
    @Basic
    @Column(name = "language_code")
    private String languageCode;
    @Basic
    @Column(name = "sub_language")
    private String subLanguage;
    @Basic
    @Column(name = "canonical_name")
    private String canonicalName;
    @Basic
    @Column(name = "content")
    private String content;
}
