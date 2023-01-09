package tech.zolhungaj.amqcontestbot;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "api")
@Data
public class ApiConfiguration {
    private String username;
    private String password;
    private boolean forceConnect;
}
