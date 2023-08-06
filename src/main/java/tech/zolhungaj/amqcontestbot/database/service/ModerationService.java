package tech.zolhungaj.amqcontestbot.database.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tech.zolhungaj.amqcontestbot.database.enums.AdminTypeEnum;
import tech.zolhungaj.amqcontestbot.database.model.AdminEntity;
import tech.zolhungaj.amqcontestbot.database.model.AdminLogEntity;
import tech.zolhungaj.amqcontestbot.database.model.BanEntity;
import tech.zolhungaj.amqcontestbot.database.model.PlayerEntity;
import tech.zolhungaj.amqcontestbot.database.repository.AdminLogRepository;
import tech.zolhungaj.amqcontestbot.database.repository.AdminRepository;
import tech.zolhungaj.amqcontestbot.database.repository.BanRepository;
import tech.zolhungaj.amqcontestbot.exceptions.IncorrectCommandUsageException;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ModerationService {
    private static final List<AdminTypeEnum> MODERATOR_TIER = List.of(AdminTypeEnum.MODERATOR, AdminTypeEnum.ADMIN, AdminTypeEnum.HOST, AdminTypeEnum.OWNER);
    private static final List<AdminTypeEnum> ADMIN_TIER = List.of(AdminTypeEnum.HOST, AdminTypeEnum.ADMIN, AdminTypeEnum.OWNER);
    private static final List<AdminTypeEnum> OWNER_TIER = List.of(AdminTypeEnum.OWNER);
    private final PlayerService playerService;
    private final AdminRepository adminRepository;
    private final AdminLogRepository logRepository;
    private final BanRepository banRepository;
    public void banCommand(String originalName, String adminOriginalName, String reason, Duration duration){
        if(!isAdmin(adminOriginalName)){
            log.error("User {} tried to ban {} but is not an admin or owner", originalName, adminOriginalName);
            return;
        }
        if(isModerator(originalName)){
            throw new IncorrectCommandUsageException("ban.error.is-moderator", originalName);
        }
        PlayerEntity player = playerService.getOrCreatePlayer(originalName);
        BanEntity ban = new BanEntity();
        ban.setPlayerId(player.getId());
        Timestamp now = now();
        ban.setStart(now);
        Timestamp expiry = Timestamp.from(now.toInstant().plus(duration));
        ban.setExpiry(expiry);
        ban.setReason(reason);
        banRepository.save(ban);
        addLog(adminOriginalName, "Banned " + originalName + " until " + expiry + " for " + reason);
    }
    public void unbanCommand(String originalName, String adminOriginalName){
        if(!isAdmin(adminOriginalName)){
            log.error("User {} tried to unban {} but is not an admin", adminOriginalName, originalName);
            return;
        }
        if(!isBanned(originalName)){
            log.info("User {} tried to unban {} but they are not banned", adminOriginalName, originalName);
            throw new IncorrectCommandUsageException("unban.error.not-banned", originalName);
        }
        Optional<PlayerEntity> player = playerService.getPlayer(originalName);
        List<BanEntity> activeBans = player
                .map(playerEntity -> banRepository.findAllByPlayerIdIsAndExpiryIsAfter(playerEntity.getId(), now()))
                .orElse(List.of());
        activeBans.forEach(ban -> ban.setExpiry(now()));
        banRepository.saveAll(activeBans);
        addLog(adminOriginalName, "Unbanned " + originalName);
    }
    public boolean isBanned(String originalName){
        Optional<PlayerEntity> player = playerService.getPlayer(originalName);
        return player
                .map(playerEntity -> banRepository.findAllByPlayerIdIsAndExpiryIsAfter(playerEntity.getId(), now()))
                .filter(list -> !list.isEmpty())
                .isPresent();
    }

    private static Timestamp now(){
        return Timestamp.from(Instant.now());
    }

    public boolean isModerator(String originalName){
        return getAdminEntity(originalName)
                .map(AdminEntity::getAdminType)
                .filter(MODERATOR_TIER::contains)
                .isPresent();
    }
    public boolean isAdmin(String originalName){
        return getAdminEntity(originalName)
                .map(AdminEntity::getAdminType)
                .filter(ADMIN_TIER::contains)
                .isPresent();
    }

    public boolean isOwner(String originalName){
        return getAdminEntity(originalName)
                .map(AdminEntity::getAdminType)
                .filter(OWNER_TIER::contains)
                .isPresent();
    }

    private Optional<AdminEntity> getAdminEntity(String originalName){
        Optional<PlayerEntity> playerEntity = playerService.getPlayer(originalName);
        return playerEntity.flatMap(player -> adminRepository.findById(player.getId()));
    }

    public void addAdminCommand(String newAdminOriginalName, String sourceOriginalName){
        if(!isAdmin(sourceOriginalName)){
            log.error("User {} tried to add admin {} but is not an admin", sourceOriginalName, newAdminOriginalName);
            return;
        }
        Optional<PlayerEntity> playerEntityOptional = playerService.getPlayer(newAdminOriginalName);
        if(playerEntityOptional.isEmpty()){
            //prevent adding admins that don't exist, which would allow someone to later create an account with that name and become an admin
            log.info("User {} tried to add admin {} but the user does not exist", sourceOriginalName, newAdminOriginalName);
            throw new IncorrectCommandUsageException("add-admin.error.user-does-not-exist", newAdminOriginalName);
        }
        PlayerEntity playerEntity = playerEntityOptional.get();
        Optional<AdminEntity> adminEntityOptional = adminRepository.findById(playerEntity.getId());
        if(adminEntityOptional.isPresent()){
            AdminEntity adminEntity = adminEntityOptional.get();
            if(AdminTypeEnum.ADMIN.equals(adminEntity.getAdminType()) || AdminTypeEnum.HOST.equals(adminEntity.getAdminType()) || AdminTypeEnum.OWNER.equals(adminEntity.getAdminType())){
                return; //easier to just keep this idempotent
            }
            adminEntity.setAdminType(AdminTypeEnum.ADMIN);
            adminRepository.save(adminEntity);
            addLog(sourceOriginalName, "promote to admin " + newAdminOriginalName);
        }else{
            AdminEntity newAdmin = new AdminEntity();
            newAdmin.setPlayerId(playerEntity.getId());
            newAdmin.setAdminType(AdminTypeEnum.ADMIN);
            adminRepository.save(newAdmin);
            addLog(sourceOriginalName, "add admin " + newAdminOriginalName);
        }
    }

    public void removeAdminCommand(String adminOriginalName, String removerOriginalName){
        if(!isAdmin(removerOriginalName)){
            log.error("User {} tried to remove admin {} but is not an admin", removerOriginalName, adminOriginalName);
            return;
        }
        Optional<PlayerEntity> playerEntityOptional = playerService.getPlayer(adminOriginalName);
        if(playerEntityOptional.isEmpty()){
            log.info("User {} tried to remove admin {} but the user does not exist", removerOriginalName, adminOriginalName);
            throw new IncorrectCommandUsageException("remove-admin.error.user-does-not-exist", adminOriginalName);
        }
        PlayerEntity playerEntity = playerEntityOptional.get();
        Optional<AdminEntity> adminEntityOptional = adminRepository.findById(playerEntity.getId());
        if(adminEntityOptional.isPresent()){
            AdminEntity adminEntity = adminEntityOptional.get();
            if(AdminTypeEnum.ADMIN.equals(adminEntity.getAdminType())){
                adminRepository.delete(adminEntityOptional.get());
                addLog(removerOriginalName, "remove admin " + adminOriginalName);
            }else{
                throw new IncorrectCommandUsageException("remove-admin.error.wrong-role", adminOriginalName, AdminTypeEnum.ADMIN.name(), adminEntity.getAdminType().name());
            }
        }else{
            throw new IncorrectCommandUsageException("remove-admin.error.not-admin", adminOriginalName);
        }
    }

    public void addModeratorCommand(String newModeratorOriginalName, String sourceOriginalName){
        if(!isModerator(newModeratorOriginalName)){
            log.error("User {} tried to add moderator {} but is not a moderator", sourceOriginalName, newModeratorOriginalName);
            return;
        }
        Optional<PlayerEntity> playerEntityOptional = playerService.getPlayer(newModeratorOriginalName);
        if(playerEntityOptional.isEmpty()){
            //prevent adding moderators that don't exist, which would allow someone to later create an account with that name and become a moderator
            log.info("User {} tried to add moderator {} but the user does not exist", sourceOriginalName, newModeratorOriginalName);
            throw new IncorrectCommandUsageException("add-moderator.error.user-does-not-exist", newModeratorOriginalName);
        }
        PlayerEntity playerEntity = playerEntityOptional.get();
        Optional<AdminEntity> adminEntityOptional = adminRepository.findById(playerEntity.getId());
        if(adminEntityOptional.isPresent()){
            AdminEntity adminEntity = adminEntityOptional.get();
            if(AdminTypeEnum.MODERATOR.equals(adminEntity.getAdminType()) || AdminTypeEnum.ADMIN.equals(adminEntity.getAdminType()) || AdminTypeEnum.HOST.equals(adminEntity.getAdminType()) || AdminTypeEnum.OWNER.equals(adminEntity.getAdminType())){
                return; //easier to just keep this idempotent
            }
            adminEntity.setAdminType(AdminTypeEnum.MODERATOR);
            adminRepository.save(adminEntity);
            addLog(sourceOriginalName, "promote to moderator " + newModeratorOriginalName);
        }else{
            AdminEntity newAdmin = new AdminEntity();
            newAdmin.setPlayerId(playerEntity.getId());
            newAdmin.setAdminType(AdminTypeEnum.MODERATOR);
            adminRepository.save(newAdmin);
            addLog(sourceOriginalName, "add moderator " + newModeratorOriginalName);
        }
    }

    public void removeModerator(String moderatorOriginalName, String removerOriginalName){
        if(!isModerator(moderatorOriginalName)){
            log.error("User {} tried to remove moderator {} but is not a moderator", removerOriginalName, moderatorOriginalName);
            return;
        }
        Optional<PlayerEntity> playerEntityOptional = playerService.getPlayer(moderatorOriginalName);
        if(playerEntityOptional.isEmpty()){
            log.info("User {} tried to remove moderator {} but the user does not exist", removerOriginalName, moderatorOriginalName);
            throw new IncorrectCommandUsageException("remove-moderator.error.user-does-not-exist", moderatorOriginalName);
        }
        PlayerEntity playerEntity = playerEntityOptional.get();
        Optional<AdminEntity> adminEntityOptional = adminRepository.findById(playerEntity.getId());
        if(adminEntityOptional.isPresent()){
            AdminEntity adminEntity = adminEntityOptional.get();
            if(AdminTypeEnum.MODERATOR.equals(adminEntity.getAdminType())){
                adminRepository.delete(adminEntityOptional.get());
                addLog(removerOriginalName, "remove moderator " + moderatorOriginalName);
            }else{
                throw new IncorrectCommandUsageException("remove-admin.error.wrong-role", moderatorOriginalName, AdminTypeEnum.MODERATOR.name(), adminEntity.getAdminType().name());
            }
        }else{
            throw new IncorrectCommandUsageException("remove-admin.error.not-moderator", moderatorOriginalName);
        }
    }

    public void addLog(String originalName, String action){
        PlayerEntity playerEntity = playerService.getPlayer(originalName).orElseThrow();
        AdminLogEntity logEntity = new AdminLogEntity();
        logEntity.setAdminId(playerEntity.getId());
        logEntity.setAction(action);
        logRepository.save(logEntity);
    }

    public void registerHost(String hostOriginalName){
        PlayerEntity host = playerService.getOrCreatePlayer(hostOriginalName);
        AdminEntity adminEntity = new AdminEntity();
        adminEntity.setPlayerId(host.getId());
        adminEntity.setAdminType(AdminTypeEnum.HOST);
        adminRepository.save(adminEntity);
    }

    public void unregisterHost(String hostOriginalName){
        PlayerEntity host = playerService.getPlayer(hostOriginalName).orElseThrow();
        adminRepository.deleteById(host.getId());
    }
}
