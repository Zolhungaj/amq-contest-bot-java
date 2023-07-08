package tech.zolhungaj.amqcontestbot.database.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tech.zolhungaj.amqcontestbot.database.enums.AdminType;
import tech.zolhungaj.amqcontestbot.database.model.AdminEntity;
import tech.zolhungaj.amqcontestbot.database.model.AdminLogEntity;
import tech.zolhungaj.amqcontestbot.database.model.PlayerEntity;
import tech.zolhungaj.amqcontestbot.database.repository.AdminLogRepository;
import tech.zolhungaj.amqcontestbot.database.repository.AdminRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ModerationService {
    private final PlayerService playerService;
    private final AdminRepository adminRepository;
    private final AdminLogRepository logRepository;
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

    public void addAdmin(String newAdminOriginalName, String sourceOriginalName){
        if(!isAdmin(sourceOriginalName) && !isOwner(sourceOriginalName)){
            log.error("User {} tried to add admin {} but is not an admin or owner", sourceOriginalName, newAdminOriginalName);
            return;
        }
        Optional<PlayerEntity> playerEntityOptional = playerService.getPlayer(newAdminOriginalName);
        if(playerEntityOptional.isEmpty()){
            //prevent adding admins that don't exist, which would allow someone to later create an account with that name and become an admin
            log.error("User {} tried to add admin {} but the user does not exist", sourceOriginalName, newAdminOriginalName);
            throw new IllegalArgumentException("User " + newAdminOriginalName + " does not exist");
        }
        PlayerEntity playerEntity = playerEntityOptional.get();
        Optional<AdminEntity> adminEntityOptional = adminRepository.findById(playerEntity.getId());
        if(adminEntityOptional.isPresent()){
            AdminEntity adminEntity = adminEntityOptional.get();
            if(AdminType.ADMIN.equals(adminEntity.getAdminType()) || AdminType.OWNER.equals(adminEntity.getAdminType())){
                return; //easier to just keep this idempotent
            }
            adminEntity.setAdminType(AdminType.ADMIN);
            adminRepository.save(adminEntity);
            addLog(sourceOriginalName, "promote to admin " + newAdminOriginalName);
        }else{
            AdminEntity newAdmin = new AdminEntity();
            newAdmin.setPlayerId(playerEntity.getId());
            newAdmin.setAdminType(AdminType.ADMIN);
            adminRepository.save(newAdmin);
            addLog(sourceOriginalName, "add admin " + newAdminOriginalName);
        }
    }

    public void removeAdmin(String adminOriginalName, String removerOriginalName){
        if(!isAdmin(removerOriginalName) && !isOwner(removerOriginalName)){
            log.error("User {} tried to remove admin {} but is not an admin or owner", removerOriginalName, adminOriginalName);
            return;
        }
        Optional<PlayerEntity> playerEntityOptional = playerService.getPlayer(adminOriginalName);
        if(playerEntityOptional.isEmpty()){
            log.error("User {} tried to remove admin {} but the user does not exist", removerOriginalName, adminOriginalName);
            throw new IllegalArgumentException("User " + adminOriginalName + " does not exist");
        }
        PlayerEntity playerEntity = playerEntityOptional.get();
        Optional<AdminEntity> adminEntityOptional = adminRepository.findById(playerEntity.getId());
        if(adminEntityOptional.isPresent()){
            AdminEntity adminEntity = adminEntityOptional.get();
            if(AdminType.ADMIN.equals(adminEntity.getAdminType())){
                adminRepository.delete(adminEntityOptional.get());
                addLog(removerOriginalName, "remove admin " + adminOriginalName);
            }else{
                throw new IllegalArgumentException("User " + adminOriginalName + " is not an ADMIN, they do however have the role " + adminEntity.getAdminType());
            }
        }else{
            throw new IllegalArgumentException("User " + adminOriginalName + " is not an admin");
        }
    }

    public void addModerator(String newModeratorOriginalName, String sourceOriginalName){
        if(!isModerator(newModeratorOriginalName) && !isAdmin(sourceOriginalName) && !isOwner(sourceOriginalName)){
            log.error("User {} tried to add moderator {} but is not an admin or owner", sourceOriginalName, newModeratorOriginalName);
            return;
        }
        Optional<PlayerEntity> playerEntityOptional = playerService.getPlayer(newModeratorOriginalName);
        if(playerEntityOptional.isEmpty()){
            //prevent adding moderators that don't exist, which would allow someone to later create an account with that name and become a moderator
            log.error("User {} tried to add moderator {} but the user does not exist", sourceOriginalName, newModeratorOriginalName);
            throw new IllegalArgumentException("User " + newModeratorOriginalName + " does not exist");
        }
        PlayerEntity playerEntity = playerEntityOptional.get();
        Optional<AdminEntity> adminEntityOptional = adminRepository.findById(playerEntity.getId());
        if(adminEntityOptional.isPresent()){
            AdminEntity adminEntity = adminEntityOptional.get();
            if(AdminType.MODERATOR.equals(adminEntity.getAdminType()) || AdminType.ADMIN.equals(adminEntity.getAdminType()) || AdminType.OWNER.equals(adminEntity.getAdminType())){
                return; //easier to just keep this idempotent
            }
            adminEntity.setAdminType(AdminType.MODERATOR);
            adminRepository.save(adminEntity);
            addLog(sourceOriginalName, "promote to moderator " + newModeratorOriginalName);
        }else{
            AdminEntity newAdmin = new AdminEntity();
            newAdmin.setPlayerId(playerEntity.getId());
            newAdmin.setAdminType(AdminType.MODERATOR);
            adminRepository.save(newAdmin);
            addLog(sourceOriginalName, "add moderator " + newModeratorOriginalName);
        }
    }

    public void removeModerator(String moderatorOriginalName, String removerOriginalName){
        if(!isModerator(moderatorOriginalName) && !isAdmin(removerOriginalName) && !isOwner(removerOriginalName)){
            log.error("User {} tried to remove moderator {} but is not a moderator, admin or owner", removerOriginalName, moderatorOriginalName);
            return;
        }
        Optional<PlayerEntity> playerEntityOptional = playerService.getPlayer(moderatorOriginalName);
        if(playerEntityOptional.isEmpty()){
            log.error("User {} tried to remove moderator {} but the user does not exist", removerOriginalName, moderatorOriginalName);
            throw new IllegalArgumentException("User " + moderatorOriginalName + " does not exist");
        }
        PlayerEntity playerEntity = playerEntityOptional.get();
        Optional<AdminEntity> adminEntityOptional = adminRepository.findById(playerEntity.getId());
        if(adminEntityOptional.isPresent()){
            AdminEntity adminEntity = adminEntityOptional.get();
            if(AdminType.MODERATOR.equals(adminEntity.getAdminType())){
                adminRepository.delete(adminEntityOptional.get());
                addLog(removerOriginalName, "remove moderator " + moderatorOriginalName);
            }else{
                throw new IllegalArgumentException("User " + moderatorOriginalName + " is not a MODERATOR, they do however have the role " + adminEntity.getAdminType());
            }
        }else{
            throw new IllegalArgumentException("User " + moderatorOriginalName + " is not a moderator");
        }
    }

    private void addLog(String originalName, String action){
        PlayerEntity playerEntity = playerService.getPlayer(originalName).orElseThrow();
        AdminLogEntity logEntity = new AdminLogEntity();
        logEntity.setAdminId(playerEntity.getId());
        logEntity.setAction(action);
        logRepository.save(logEntity);
    }
}
