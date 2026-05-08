package net.kunmc.lab.configlib.exception;

import net.kunmc.lab.commandlib.CommonCommandContext;
import net.kunmc.lab.configlib.ConfigCommandDescriptions;
import net.kunmc.lab.configlib.util.ListUtil;

import java.util.List;
import java.util.function.Consumer;

public class InvalidValueException extends Exception {
    private final Consumer<CommonCommandContext<?, ?>> messageSender;

    public InvalidValueException() {
        this(ctx -> ctx.sendFailure(ConfigCommandDescriptions.describe(ctx,
                                                                       ConfigCommandDescriptions.Key.INVALID_VALUE)),
             "Invalid value.");
    }

    public InvalidValueException(String message, String... messages) {
        this(ListUtil.asList(message, messages),
             String.join(System.lineSeparator(), ListUtil.asList(message, messages)));
    }

    public InvalidValueException(List<String> messages) {
        this(messages, String.join(System.lineSeparator(), messages));
    }

    private InvalidValueException(List<String> messages, String logMessage) {
        super(logMessage);
        this.messageSender = ctx -> messages.forEach(ctx::sendFailure);
    }

    public InvalidValueException(Consumer<CommonCommandContext<?, ?>> messageSender) {
        this(messageSender, "Custom validation message");
    }

    public InvalidValueException(Consumer<CommonCommandContext<?, ?>> messageSender, String logMessage) {
        super(logMessage);
        this.messageSender = messageSender;
    }

    public void sendMessage(CommonCommandContext<?, ?> ctx) {
        this.messageSender.accept(ctx);
    }
}
