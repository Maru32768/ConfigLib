package net.kunmc.lab.configlib.value.collection;

import net.kunmc.lab.commandlib.argument.CommonStringArgument;
import net.kunmc.lab.commandlib.exception.ArgumentValidationException;
import net.kunmc.lab.configlib.ArgumentDefinition;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class StringListValue extends ListValue<String, StringListValue> {
    private final transient List<String> allowableStringList = new ArrayList<>();
    protected transient String name = "string";
    protected transient CommonStringArgument.Type type = CommonStringArgument.Type.PHRASE;

    public StringListValue(String... strings) {
        this(Arrays.stream(strings)
                   .collect(Collectors.toList()));
    }

    public StringListValue(List<String> value) {
        super(value);
    }

    public StringListValue name(@NotNull String name) {
        this.name = name;
        return this;
    }

    public StringListValue type(@NotNull CommonStringArgument.Type type) {
        this.type = type;
        return this;
    }

    public StringListValue addAllowableString(@NotNull String s) {
        allowableStringList.add(s);
        return this;
    }

    @Override
    protected List<ArgumentDefinition<List<String>>> argumentDefinitionsForAdd() {
        return List.of(new ArgumentDefinition<>(new CommonStringArgument<>(name, type).suggestionAction(sb -> {
                                                                                          allowableStringList.forEach(sb::suggest);
                                                                                      })
                                                                                      .validator((x, ctx) -> {
                                                                                          if (allowableStringList.stream()
                                                                                                                 .noneMatch(
                                                                                                                         s -> s.equals(
                                                                                                                                 x))) {
                                                                                              throw new ArgumentValidationException(
                                                                                                      x + "は不正な引数です");
                                                                                          }
                                                                                      }), (s, ctx) -> {
            return List.of(s);
        }));
    }

    @Override
    protected List<ArgumentDefinition<List<String>>> argumentDefinitionsForRemove() {
        return List.of(new ArgumentDefinition<>(new CommonStringArgument<>(name).suggestionAction(sb -> {
            value.forEach(sb::suggest);
        }), (s, ctx) -> {
            return List.of(s);
        }));
    }

    @Override
    protected String elementToString(String s) {
        return s;
    }
}
