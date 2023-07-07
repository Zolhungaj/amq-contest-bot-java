package tech.zolhungaj.amqcontestbot.database.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.zolhungaj.amqcontestbot.database.model.AdminEntity;

public interface AdminRepository extends JpaRepository<AdminEntity, Integer> {
}
