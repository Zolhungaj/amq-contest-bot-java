package tech.zolhungaj.amqcontestbot.moderation;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tech.zolhungaj.amqapi.servercommands.globalstate.LoginComplete;
import tech.zolhungaj.amqcontestbot.ApiManager;
import tech.zolhungaj.amqcontestbot.commands.ChatCommands;
import tech.zolhungaj.amqcontestbot.commands.Grant;
import tech.zolhungaj.amqcontestbot.database.service.ModerationService;
import tech.zolhungaj.amqcontestbot.exceptions.IncorrectArgumentCountException;
import tech.zolhungaj.amqcontestbot.exceptions.IncorrectCommandUsageException;

@Component
@RequiredArgsConstructor
public class AdministratorManager {
    private final ChatCommands chatCommands;
    private final ApiManager api;
    private final ModerationService moderationService;
    private final NameResolver nameResolver;
    @PostConstruct
    private void init(){
        registerAdmin();
        registerModerator();
        api.on(LoginComplete.class, loginComplete -> nameResolver
                .resolveOriginalNameAsync(loginComplete.selfName())
                .thenAccept(moderationService::registerHost)
        );
    }

    @PreDestroy
    private void unregisterHost(){
        moderationService.unregisterHost(nameResolver.resolveOriginalName(api.getSelfName()));
    }

    private void registerAdmin(){
        chatCommands.register((sender, arguments) -> {
            if(arguments.size() != 1){
                throw new IncorrectArgumentCountException(1);
            }
            addAdmin(sender, arguments.getFirst());
        }, Grant.ADMIN, "addadministrator", "addadmin");
        chatCommands.register((sender, arguments) -> {
            if(arguments.size() != 1){
                throw new IncorrectArgumentCountException(1);
            }
            removeAdmin(sender, arguments.getFirst());
        }, Grant.ADMIN, "removeadministrator","removeadmin");
    }

    private void registerModerator(){
        chatCommands.register((sender, arguments) -> {
            if(arguments.size() != 1){
                throw new IncorrectArgumentCountException(1);
            }
            addModerator(sender, arguments.getFirst());
        }, Grant.MODERATOR, "addmoderator", "addmod");
        chatCommands.register((sender, arguments) -> {
            if(arguments.size() != 1){
                throw new IncorrectArgumentCountException(1);
            }
            removeModerator(sender, arguments.getFirst());
        }, Grant.MODERATOR, "removemoderator", "removemod");
    }

    private void addAdmin(String sender, String receiver){
        String senderTrueName = nameResolver.resolveOriginalName(sender);
        String receiverTrueName = nameResolver.resolveOriginalName(receiver);
        moderationService.addAdminCommand(receiverTrueName, senderTrueName);
    }

    private void removeAdmin(String remover, String admin){
        if(admin.equals(remover)){
            throw new IncorrectCommandUsageException("admin.error.remove-self");
        }
        String removerTrueName = nameResolver.resolveOriginalName(remover);
        String adminTrueName = nameResolver.resolveOriginalName(admin);
        moderationService.removeAdminCommand(adminTrueName, removerTrueName);
    }

    private void addModerator(String sender, String receiver){
        String senderTrueName = nameResolver.resolveOriginalName(sender);
        String receiverTrueName = nameResolver.resolveOriginalName(receiver);
        moderationService.addModeratorCommand(receiverTrueName, senderTrueName);
    }

    private void removeModerator(String remover, String moderator){
        String removerTrueName = nameResolver.resolveOriginalName(remover);
        String moderatorTrueName = nameResolver.resolveOriginalName(moderator);
        moderationService.removeModerator(moderatorTrueName, removerTrueName);
    }
}
