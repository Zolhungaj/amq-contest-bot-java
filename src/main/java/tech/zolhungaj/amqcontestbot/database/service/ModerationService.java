package tech.zolhungaj.amqcontestbot.database.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tech.zolhungaj.amqcontestbot.database.enums.AdminType;
import tech.zolhungaj.amqcontestbot.database.model.AdminEntity;
import tech.zolhungaj.amqcontestbot.database.model.PlayerEntity;
import tech.zolhungaj.amqcontestbot.database.repository.AdminRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ModerationService {
    private final PlayerService playerService;
    private final AdminRepository adminRepository;
    public void ban(String originalName){
        //TODO:implement
    }
    public void unban(String originalName){
        //TODO:implement
    }
    public boolean isBanned(String originalName){
        return false; //TODO:implement
    }

    public boolean isModerator(String originalName){
        return getAdminEntity(originalName)
                .filter(entity -> AdminType.MODERATOR.equals(entity.getAdminType()))
                .isPresent();
    }
    public boolean isAdmin(String originalName){
        return getAdminEntity(originalName)
                .filter(entity -> AdminType.ADMIN.equals(entity.getAdminType()))
                .isPresent();
    }

    public boolean isOwner(String originalName){
        return getAdminEntity(originalName)
                .filter(entity -> AdminType.OWNER.equals(entity.getAdminType()))
                .isPresent();
    }

    private Optional<AdminEntity> getAdminEntity(String originalName){
        Optional<PlayerEntity> playerEntity = playerService.getPlayer(originalName);
        return playerEntity.flatMap(player -> adminRepository.findById(player.getId()));
    }

    public void addAdmin(String source, String newAdmin){
        //TODO: implement
    }

    public void removeAdmin(String remover, String admin){
        //TODO: implement
    }

    public void addModerator(String source, String newModerator){
        //TODO: implement
    }

    public void removeModerator(String remover, String moderator){
        //TODO: implement
    }
}
