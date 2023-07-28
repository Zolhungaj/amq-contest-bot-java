package tech.zolhungaj.amqcontestbot;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tech.zolhungaj.amqapi.AmqApi;
import tech.zolhungaj.amqapi.clientcommands.ClientCommand;
import tech.zolhungaj.amqapi.servercommands.Command;
import tech.zolhungaj.amqapi.servercommands.globalstate.LoginComplete;

import java.util.function.Consumer;
import java.util.function.Predicate;

@Component
public class ApiManager {
    private final AmqApi api;
    @Getter
    private String selfName;
    public ApiManager(@Autowired ApiConfiguration configuration){
        this.api = new AmqApi(configuration.getUsername(), configuration.getPassword(), configuration.isForceConnect());
        this.on(LoginComplete.class, loginComplete -> this.selfName = loginComplete.selfName());
    }

    public void onAllCommands(Consumer<Command> consumer){
        this.api.onAllCommands(consumer);
    }

    public <T extends Command> void on(Class<T> commandClass, Consumer<T> consumer){
        this.api.on(commandClass, consumer);
    }

    public <T extends Command> void once(Class<T> commandClass, Predicate<T> predicate){
        this.api.once(commandClass, predicate);
    }

    public void sendCommand(ClientCommand command){
        this.api.sendCommand(command);
    }

    private Thread thread;

    public void start(){
        if(thread == null){
            thread = new Thread(api);
            thread.start();
        }
    }

    public void stop(){
        if(thread != null){
            thread.interrupt();
            thread = null;
        }
    }

    public boolean isAlive(){
        return thread != null && thread.isAlive();
    }
}
