package net.kunmc.lab.configlib.value;

import net.kunmc.lab.commandlib.CommonCommandContext;
import net.kunmc.lab.commandlib.argument.CommonStringArgument;
import net.kunmc.lab.commandlib.exception.ArgumentValidationException;
import net.kunmc.lab.commandlib.suggestion.SuggestionAction;
import net.kunmc.lab.configlib.ArgumentDefinition;
import net.kunmc.lab.configlib.SingleValue;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class StringValue extends SingleValue<String, StringValue> {
    protected final transient List<String> allowableStringList = new ArrayList<>();
    private final int min;
    private final int max;
    protected transient String name = "string";
    protected transient CommonStringArgument.Type type = CommonStringArgument.Type.PHRASE;
    protected transient SuggestionAction<CommonCommandContext<?, ?>> suggestionAction = sb -> {
        allowableStringList.forEach(sb::suggest);
    };

    public StringValue(String value) {
        this(value, 0, Integer.MAX_VALUE);
    }

    public StringValue(String value, int min, int max) {
        super(value);
        this.min = min;
        this.max = max;
    }

    public StringValue name(@NotNull String name) {
        this.name = name;
        return this;
    }

    public StringValue type(@NotNull CommonStringArgument.Type type) {
        this.type = type;
        return this;
    }

    public StringValue addAllowableString(@NotNull String s) {
        allowableStringList.add(s);
        return this;
    }

    public StringValue suggestionAction(@NotNull SuggestionAction<CommonCommandContext<?, ?>> action) {
        this.suggestionAction = action;
        return this;
    }

    @Override
    protected List<ArgumentDefinition<String>> argumentDefinitions() {
        return List.of(new ArgumentDefinition<>(new CommonStringArgument<>(name,
                                                                           type).suggestionAction(suggestionAction)
                                                                                .validator((x, ctx) -> {
                                                                                    if (!allowableStringList.isEmpty()) {
                                                                                        if (allowableStringList.stream()
                                                                                                               .noneMatch(
                                                                                                                       s -> s.equals(
                                                                                                                               x))) {
                                                                                            throw new ArgumentValidationException(
                                                                                                    allowableStringList + "の中から文字列を入力してください");
                                                                                        }
                                                                                    }

                                                                                    if (x.length() < min || x.length() > max) {
                                                                                        throw new ArgumentValidationException(
                                                                                                min + "以上" + max + "以下の文字数で入力してください");
                                                                                    }
                                                                                }), (s, ctx) -> {
            return s;
        }));
    }

    @Override
    protected String valueToString(String s) {
        return s;
    }

}
