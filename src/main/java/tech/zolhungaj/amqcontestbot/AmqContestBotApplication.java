package tech.zolhungaj.amqcontestbot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import tech.zolhungaj.amqapi.AmqApi;
import tech.zolhungaj.amqapi.servercommands.globalstate.OnlinePlayerCountChange;

@SpringBootApplication
@Slf4j
public class AmqContestBotApplication implements ApplicationRunner {

	public static void main(String[] args) {
		SpringApplication.run(AmqContestBotApplication.class, args);
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		String username = args.getOptionValues("username").get(0);
		String password = args.getOptionValues("password").get(0);
		boolean force = args.getOptionValues("force") != null;
		var api = new AmqApi(username, password, force);

		api.on(command -> {
			log.info("Received command {}", command);
			return true;
		});
		api.on(command -> {
			if(command instanceof OnlinePlayerCountChange o){
				log.info("Players online changed: {}", o.count());
			}
			return true;
		});
		Thread apiThread = new Thread(api);
		apiThread.start();
		apiThread.join();
		System.exit(0);
	}
}
