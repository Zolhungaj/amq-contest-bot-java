package tech.zolhungaj.amqcontestbot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tech.zolhungaj.amqapi.AmqApi;
import tech.zolhungaj.amqapi.EventHandler;
import tech.zolhungaj.amqapi.clientcommands.ClientCommand;
import tech.zolhungaj.amqapi.servercommands.globalstate.LoginComplete;

@Component
public class ApiManager {
    private final AmqApi api;
    private String selfName;
    public ApiManager(@Autowired ApiConfiguration configuration){
        this.api = new AmqApi(configuration.getUsername(), configuration.getPassword(), configuration.isForceConnect());
        this.on(command -> {
            if(command instanceof LoginComplete loginComplete){
                selfName = loginComplete.selfName();
                return true;
            }
            return false;
        });
    }

    public void on(EventHandler handler){
        this.api.on(handler);
    }

    public void once(EventHandler handler){
        this.api.once(handler);
    }

    public void sendCommand(ClientCommand command){
        this.api.sendCommand(command);
    }

    public long getPing(){
        return this.api.getCurrentPing();
    }

    public String getSelfName(){
        return this.selfName;
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
