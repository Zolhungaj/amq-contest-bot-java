package tech.zolhungaj.amqcontestbot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import tech.zolhungaj.amqapi.servercommands.globalstate.OnlinePlayerCountChange;

@SpringBootApplication
@Slf4j
public class AmqContestBotApplication implements ApplicationRunner {

	public static void main(String[] args) {
		SpringApplication.run(AmqContestBotApplication.class, args);
	}
	private final ApiManager api;

	public AmqContestBotApplication(@Autowired ApiManager api){
		this.api = api;
	}

	@Override
	public void run(ApplicationArguments args) {
		api.on(command -> {
			log.info("Main received command {}", command);
			return true;
		});
		api.on(command -> {
			if(command instanceof OnlinePlayerCountChange o){
				log.info("Players online changed: {}", o.count());
			}
			return true;
		});
		api.start();
	}
}
