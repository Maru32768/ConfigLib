package net.kunmc.lab.configlib.value.map;

import net.kunmc.lab.commandlib.argument.CommonStringArgument;
import net.kunmc.lab.commandlib.exception.ArgumentValidationException;
import net.kunmc.lab.configlib.ArgumentDefinition;
import net.kunmc.lab.configlib.MapValue;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class String2ObjectMapValue<V, T extends String2ObjectMapValue<V, T>> extends MapValue<String, V, T> {
    private final transient List<String> allowableKeyStringList = new ArrayList<>();

    public String2ObjectMapValue(Map<String, V> value) {
        super(value);
    }

    public T addAllowableKeyString(@NotNull String s) {
        allowableKeyStringList.add(s);
        return ((T) this);
    }

    protected ArgumentDefinition<String> keyArgumentDefinitionForPut() {
        return new ArgumentDefinition<>(new CommonStringArgument<>("string",
                                                                   CommonStringArgument.Type.PHRASE_QUOTED).suggestionAction(
                                                                                                                   sb -> {
                                                                                                                       if (allowableKeyStringList.isEmpty()) {
                                                                                                                           keySet().forEach(sb::suggest);
                                                                                                                       } else {
                                                                                                                           allowableKeyStringList.forEach(sb::suggest);
                                                                                                                       }
                                                                                                                   })
                                                                                                           .validator((x, ctx) -> {
                                                                                                               if (allowableKeyStringList.stream()
                                                                                                                                         .noneMatch(
                                                                                                                                                 s -> s.equals(
                                                                                                                                                         x))) {
                                                                                                                   throw new ArgumentValidationException(
                                                                                                                           x + "は不正な引数でぁE");
                                                                                                               }
                                                                                                           }),
                                        (name, ctx) -> {
                                            return name;
                                        });
    }

    @Override
    protected List<ArgumentDefinition<String>> argumentDefinitionsForRemove() {
        return List.of(new ArgumentDefinition<>(new CommonStringArgument<>("string",
                                                                           CommonStringArgument.Type.PHRASE).suggestionAction(
                sb -> {
                    keySet().forEach(sb::suggest);
                }), (name, ctx) -> {
            return name;
        }));
    }

    @Override
    protected String keyToString(String s) {
        return s;
    }
}
