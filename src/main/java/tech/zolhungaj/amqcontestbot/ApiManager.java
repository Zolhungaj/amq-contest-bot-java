package tech.zolhungaj.amqcontestbot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tech.zolhungaj.amqapi.AmqApi;
import tech.zolhungaj.amqapi.EventHandler;
import tech.zolhungaj.amqapi.clientcommands.ClientCommand;

@Component
public class ApiManager {
    private final AmqApi api;
    public ApiManager(@Autowired ApiConfiguration configuration){
        this.api = new AmqApi(configuration.getUsername(), configuration.getPassword(), configuration.isForceConnect());
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
