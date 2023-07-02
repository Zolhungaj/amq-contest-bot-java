package tech.zolhungaj.amqcontestbot.moderation;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tech.zolhungaj.amqapi.servercommands.globalstate.LoginComplete;
import tech.zolhungaj.amqcontestbot.ApiManager;
import tech.zolhungaj.amqcontestbot.chat.ChatCommands;
import tech.zolhungaj.amqcontestbot.database.service.ModerationService;

import java.util.concurrent.CompletableFuture;

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
        api.on(LoginComplete.class, loginComplete -> CompletableFuture.runAsync(
                () -> this.addAdmin(api.getSelfName(), api.getSelfName()))
        );
    }

    private void registerAdmin(){
        chatCommands.register((sender, arguments) -> {
            if(arguments.size() == 1){
                addAdmin(sender, arguments.get(0));
            }else{
                throw new IllegalArgumentException();
            }
        }, ChatCommands.Grant.ADMIN, "addadministrator", "addadmin");
        chatCommands.register((sender, arguments) -> {
            if(arguments.size() == 1){
                removeAdmin(sender, arguments.get(0));
            }else{
                throw new IllegalArgumentException();
            }
        }, ChatCommands.Grant.ADMIN, "removeadministrator","removeadmin");
    }

    private void registerModerator(){
        chatCommands.register((sender, arguments) -> {
            if(arguments.size() == 1){
                addModerator(sender, arguments.get(0));
            }else{
                throw new IllegalArgumentException();
            }
        }, ChatCommands.Grant.ADMIN, "addmoderator", "addmod");
        chatCommands.register((sender, arguments) -> {
            if(arguments.size() == 1){
                removeModerator(sender, arguments.get(0));
            }else{
                throw new IllegalArgumentException();
            }
        }, ChatCommands.Grant.ADMIN, "removemoderator", "removemod");
    }

    private void addAdmin(String sender, String receiver){
        String senderTrueName = nameResolver.resolveOriginalName(sender);
        String receiverTrueName = nameResolver.resolveOriginalName(receiver);
        moderationService.addAdmin(senderTrueName, receiverTrueName);
    }

    private void removeAdmin(String remover, String admin){
        if(admin.equals(remover)){
            throw new IllegalArgumentException("Cannot remove self");
        }
        if(admin.equals(api.getSelfName())){
            throw new IllegalArgumentException("Cannot remove bot from admin role");
        }
        String removerTrueName = nameResolver.resolveOriginalName(remover);
        String adminTrueName = nameResolver.resolveOriginalName(admin);
        moderationService.removeAdmin(removerTrueName, adminTrueName);
    }

    private void addModerator(String sender, String receiver){
        String senderTrueName = nameResolver.resolveOriginalName(sender);
        String receiverTrueName = nameResolver.resolveOriginalName(receiver);
        moderationService.addModerator(senderTrueName, receiverTrueName);
    }

    private void removeModerator(String remover, String moderator){
        String removerTrueName = nameResolver.resolveOriginalName(remover);
        String moderatorTrueName = nameResolver.resolveOriginalName(moderator);
        moderationService.removeModerator(removerTrueName, moderatorTrueName);
    }
}
