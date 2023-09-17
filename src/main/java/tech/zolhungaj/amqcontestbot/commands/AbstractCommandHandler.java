package tech.zolhungaj.amqcontestbot.commands;

import lombok.RequiredArgsConstructor;
import tech.zolhungaj.amqcontestbot.database.service.ModerationService;
import tech.zolhungaj.amqcontestbot.exceptions.CommandAccessDeniedException;
import tech.zolhungaj.amqcontestbot.exceptions.IncorrectCommandUsageException;
import tech.zolhungaj.amqcontestbot.moderation.NameResolver;

import java.util.List;
import java.util.function.Predicate;

@RequiredArgsConstructor
public abstract class AbstractCommandHandler implements Runnable{
    private final Command command;
    protected final String sender;
    private final List<String> arguments;
    private final ModerationService moderationService;
    private final NameResolver nameResolver;
    @Override
    public void run() {
        try{
            execute();
        }catch(IncorrectCommandUsageException e){
            handleIncorrectCommandUsage(e.getI18nIdentifier(), e.getArguments());
        }catch (IllegalArgumentException e){
            handleIllegalArgumentException(command.i18nCanonicalNameUsage());
        }
    }

    protected abstract void handleIncorrectCommandUsage(String i18nIdentifier, List<String> arguments);
    protected abstract void handleIllegalArgumentException(String i18nCanonicalNameUsage);

    private void execute(){
        Predicate<String> hasPermission = switch (command.grant()){
            case NONE -> s -> true;
            case MODERATOR -> moderationService::isModerator;
            case ADMIN -> moderationService::isAdmin;
            case OWNER -> moderationService::isOwner;
        };
        String originalName = nameResolver.resolveOriginalName(sender);
        if(hasPermission.test(originalName)){
            command.handler().accept(sender, arguments);
        }else{
            throw new CommandAccessDeniedException(sender, command.commandName(), command.grant());
        }
    }
}
