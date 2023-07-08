package tech.zolhungaj.amqcontestbot.database.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.zolhungaj.amqcontestbot.database.model.BanEntity;

import java.sql.Timestamp;
import java.util.List;

@Repository
public interface BanRepository extends JpaRepository<BanEntity, Integer> {
    List<BanEntity> findAllByPlayerIdIsAndExpiryIsAfter(Integer playerId, Timestamp expiry);
}
