package tech.zolhungaj.amqcontestbot.database.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tech.zolhungaj.amqcontestbot.database.model.MessageEntity;
import tech.zolhungaj.amqcontestbot.database.model.PlayerEntity;
import tech.zolhungaj.amqcontestbot.database.repository.MessageRepository;

@Service
@RequiredArgsConstructor
public class MessageService {
    private final MessageRepository repository;

    public void save(PlayerEntity player, String content, int roomId, int messageId) {
        MessageEntity entity = new MessageEntity();
        entity.setPlayer(player);
        entity.setContent(content);
        entity.setRoomId(roomId);
        entity.setRoomMessageId(messageId);
        repository.save(entity);
    }
}
