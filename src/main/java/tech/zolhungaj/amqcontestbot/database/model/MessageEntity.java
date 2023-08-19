package tech.zolhungaj.amqcontestbot.database.model;

import jakarta.persistence.*;
import lombok.Data;
@Entity
@Data
@Table(name = "message", schema = "public", catalog = "amq_contest_bot")
public class MessageEntity {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id")
    private int id;
    @ManyToOne
    @JoinColumn(name = "player_id", referencedColumnName = "id")
    private PlayerEntity player;
    @Basic
    @Column(name = "content")
    private String content;
    @Basic
    @Column(name = "room_id")
    private int roomId;
    @Basic
    @Column(name = "room_message_id")
    private int roomMessageId;
}
