package tech.zolhungaj.amqcontestbot.commands;

import lombok.NonNull;

import java.util.List;
import java.util.function.BiConsumer;

record Command(
        @NonNull String commandName,
        @NonNull Grant grant,
        @NonNull BiConsumer<String, List<String>> handler,
        @NonNull List<String> aliases) {
    public String aliasesToString() {
        return String.join(", ", aliases);
    }

    public String i18nCanonicalName() {
        return "command.".concat(commandName);
    }

    public String i18nCanonicalNameDescription() {
        return i18nCanonicalName().concat(".description");
    }

    public String i18nCanonicalNameUsage() {
        return i18nCanonicalName().concat(".usage");
    }
}
