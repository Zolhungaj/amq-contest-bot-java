package tech.zolhungaj.amqcontestbot;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tech.zolhungaj.amqapi.AmqApi;
import tech.zolhungaj.amqapi.servercommands.CommandType;
import tech.zolhungaj.amqapi.servercommands.globalstate.LoginComplete;

import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Slf4j
@Component
@EnableScheduling
public class ApiManager {
    private static final long TIMEOUT_IN_MINUTES = 2;
    private final AmqApi api;
    @Getter
    private String selfName;
    private Instant lastCommandReceived = Instant.now();
    public ApiManager(@Autowired ApiConfiguration configuration){
        this.api = new AmqApi(configuration.getUsername(), configuration.getPassword(), configuration.isForceConnect());
        this.on(LoginComplete.class, loginComplete -> this.selfName = loginComplete.selfName());
        this.onAllCommands(command -> lastCommandReceived = Instant.now());
    }

    @Scheduled(fixedRate = TIMEOUT_IN_MINUTES, initialDelay = TIMEOUT_IN_MINUTES, timeUnit = TimeUnit.MINUTES)
    private void checkConnection(){
        if(lastCommandReceived.plus(TIMEOUT_IN_MINUTES, TimeUnit.MINUTES.toChronoUnit()).isBefore(Instant.now())){
            this.stop();
            this.start();
        }
    }

    public void onAllCommands(Consumer<Object> consumer){
        this.api.onAllCommands(consumer);
    }

    public void onAllJsonCommands(Consumer<org.json.JSONObject> consumer){
        this.api.onAllJson(consumer);
    }

    public void registerCommand(Class<@CommandType ?> clazz){
        this.api.registerCommand(clazz);
    }
    public <T> void on(Class<T> commandClass, Consumer<T> consumer){
        this.api.on(commandClass, consumer);
    }

    public <T> void once(Class<T> commandClass, Predicate<T> predicate){
        this.api.once(commandClass, predicate);
    }

    public void sendCommand(Object command){
        this.api.sendCommand(command);
    }

    private Thread thread;

    public void start(){
        if(thread == null){
            thread = new Thread(api);
            lastCommandReceived = Instant.now();
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
