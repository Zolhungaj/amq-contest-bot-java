package tech.zolhungaj.amqcontestbot.bonus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class Patreon {
    public boolean isPatreon(String originalName) {
        log.info("Checking if {} is a patreon", originalName);
        return false;
    }
}
