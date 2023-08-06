package tech.zolhungaj.amqcontestbot.gamemode;

import java.time.Duration;

public record AnswerResult(
        int score,
        boolean correct,
        Duration answerTime
) {
}
