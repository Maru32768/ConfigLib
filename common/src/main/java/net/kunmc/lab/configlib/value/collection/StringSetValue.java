package net.kunmc.lab.configlib.value.collection;

import net.kunmc.lab.commandlib.argument.CommonStringArgument;
import net.kunmc.lab.commandlib.exception.ArgumentValidationException;
import net.kunmc.lab.configlib.ArgumentDefinition;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StringSetValue extends SetValue<String, StringSetValue> {
    private final transient List<String> allowableStringList = new ArrayList<>();
    protected transient String name = "string";
    protected transient CommonStringArgument.Type type = CommonStringArgument.Type.PHRASE;

    public StringSetValue(String... strings) {
        this(new HashSet<>(List.of(strings)));
    }

    public StringSetValue(@NotNull Set<String> value) {
        super(value);
    }

    public StringSetValue addAllowableString(@NotNull String s) {
        allowableStringList.add(s);
        return this;
    }

    public StringSetValue name(@NotNull String name) {
        this.name = name;
        return this;
    }

    public StringSetValue type(@NotNull CommonStringArgument.Type type) {
        this.type = type;
        return this;
    }

    @Override
    protected List<ArgumentDefinition<Set<String>>> argumentDefinitionsForAdd() {
        return List.of(new ArgumentDefinition<>(new CommonStringArgument<>(name, type).suggestionAction(sb -> {
                                                                                          allowableStringList.stream()
                                                                                                             .filter(s -> !value.contains(s))
                                                                                                             .forEach(sb::suggest);
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
            return Set.of(s);
        }));
    }

    @Override
    protected List<ArgumentDefinition<Set<String>>> argumentDefinitionsForRemove() {
        return List.of(new ArgumentDefinition<>(new CommonStringArgument<>(name).suggestionAction(sb -> {
            value.forEach(sb::suggest);
        }), (s, ctx) -> {
            return Set.of(s);
        }));
    }

    @Override
    protected String elementToString(String s) {
        return s;
    }
}
