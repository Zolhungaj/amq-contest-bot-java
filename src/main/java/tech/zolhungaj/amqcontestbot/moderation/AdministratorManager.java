package tech.zolhungaj.amqcontestbot.moderation;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tech.zolhungaj.amqapi.servercommands.globalstate.LoginComplete;
import tech.zolhungaj.amqcontestbot.ApiManager;
import tech.zolhungaj.amqcontestbot.chat.ChatCommands;
import tech.zolhungaj.amqcontestbot.repository.PlayerService;

@Component
@RequiredArgsConstructor
public class AdministratorManager {
    private final ChatCommands chatCommands;
    private final ApiManager api;
    private final PlayerService playerService;
    private final NameResolver nameResolver;
    private String selfName = "";

    @PostConstruct
    public void init(){
        registerAdmin();
        registerModerator();
        api.on(command -> {
            if(command instanceof LoginComplete loginComplete){
                selfName = loginComplete.selfName();
                addAdmin(selfName, selfName);
                return true;
            }
            return false;
        });
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
        String senderTrueName = nameResolver.getTrueNameBlocking(sender);
        String receiverTrueName = nameResolver.getTrueNameBlocking(receiver);
        playerService.addAdmin(senderTrueName, receiverTrueName);
    }

    private void removeAdmin(String remover, String admin){
        if(admin.equals(remover)){
            throw new IllegalArgumentException("Cannot remove self");
        }
        if(admin.equals(selfName)){
            throw new IllegalArgumentException("Cannot remove bot from admin role");
        }
        String removerTrueName = nameResolver.getTrueNameBlocking(remover);
        String adminTrueName = nameResolver.getTrueNameBlocking(admin);
        playerService.removeAdmin(removerTrueName, adminTrueName);
    }

    private void addModerator(String sender, String receiver){
        String senderTrueName = nameResolver.getTrueNameBlocking(sender);
        String receiverTrueName = nameResolver.getTrueNameBlocking(receiver);
        playerService.addModerator(senderTrueName, receiverTrueName);
    }

    private void removeModerator(String remover, String moderator){
        String removerTrueName = nameResolver.getTrueNameBlocking(remover);
        String moderatorTrueName = nameResolver.getTrueNameBlocking(moderator);
        playerService.removeModerator(removerTrueName, moderatorTrueName);
    }
}
