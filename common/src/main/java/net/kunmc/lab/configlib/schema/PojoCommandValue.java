package net.kunmc.lab.configlib.schema;

import net.kunmc.lab.commandlib.argument.*;
import net.kunmc.lab.configlib.ArgumentDefinition;
import net.kunmc.lab.configlib.SingleValue;
import net.kunmc.lab.configlib.annotation.Range;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

final class PojoCommandValue extends SingleValue<Object, PojoCommandValue> {
    // Keep POJO command mutation to a fixed scalar set. Custom parsing,
    // completion, Minecraft-specific types, and operation commands should be
    // modeled with explicit Value types instead of extending this table.
    private static final Map<Class<?>, Class<?>> PRIMITIVE_TO_WRAPPER = Map.of(boolean.class,
                                                                               Boolean.class,
                                                                               byte.class,
                                                                               Byte.class,
                                                                               char.class,
                                                                               Character.class,
                                                                               double.class,
                                                                               Double.class,
                                                                               float.class,
                                                                               Float.class,
                                                                               int.class,
                                                                               Integer.class,
                                                                               long.class,
                                                                               Long.class,
                                                                               short.class,
                                                                               Short.class,
                                                                               void.class,
                                                                               Void.class);
    private static final List<ArgumentRule> ARGUMENT_RULES = List.of(new ArgumentRule(type -> type == String.class,
                                                                                      PojoCommandValue::stringArguments),
                                                                     new ArgumentRule(type -> type == Boolean.class,
                                                                                      PojoCommandValue::booleanArguments),
                                                                     new ArgumentRule(type -> type == Integer.class,
                                                                                      PojoCommandValue::integerArguments),
                                                                     new ArgumentRule(type -> type == Float.class,
                                                                                      PojoCommandValue::floatArguments),
                                                                     new ArgumentRule(type -> type == Double.class,
                                                                                      PojoCommandValue::doubleArguments),
                                                                     new ArgumentRule(type -> type == Long.class,
                                                                                      PojoCommandValue::longArguments),
                                                                     new ArgumentRule(Class::isEnum,
                                                                                      PojoCommandValue::enumArguments));

    private final Class<?> type;
    private final Field field;


    private PojoCommandValue(Object value, Class<?> type, Field field) {
        super(value);
        this.type = type;
        this.field = field;
    }

    @Nullable
    static PojoCommandValue from(Field[] parentChain, Field field, Object value) {
        if (parentChain.length == 0 && Modifier.isFinal(field.getModifiers())) {
            return null;
        }
        Class<?> type = wrap(field.getType());
        if (findRule(type) != null) {
            return new PojoCommandValue(value, type, field);
        }
        return null;
    }

    @SuppressWarnings({"unchecked"})
    @Override
    protected List<ArgumentDefinition<?>> argumentDefinitions() {
        ArgumentRule rule = findRule(type);
        if (rule != null) {
            return rule.argumentFactory.apply(field);
        }
        throw new IllegalStateException("Unsupported POJO command value type: " + type.getName());
    }

    @Override
    protected String valueToString(Object value) {
        return String.valueOf(value);
    }

    private static Class<?> wrap(Class<?> type) {
        return PRIMITIVE_TO_WRAPPER.getOrDefault(type, type);
    }

    private static ArgumentRule findRule(Class<?> type) {
        return ARGUMENT_RULES.stream()
                             .filter(rule -> rule.supports(type))
                             .findFirst()
                             .orElse(null);
    }

    private static List<ArgumentDefinition<?>> stringArguments(Field field) {
        return List.of(new ArgumentDefinition<>(new CommonStringArgument(field.getName(),
                                                                         CommonStringArgument.Type.PHRASE)));
    }

    private static List<ArgumentDefinition<?>> booleanArguments(Field field) {
        return List.of(new ArgumentDefinition<>(new CommonBooleanArgument(field.getName())));
    }

    private static List<ArgumentDefinition<?>> integerArguments(Field field) {
        Range range = field.getAnnotation(Range.class);
        int min = range == null ? Integer.MIN_VALUE : (int) Math.ceil(range.min());
        int max = range == null ? Integer.MAX_VALUE : (int) Math.floor(range.max());
        return List.of(new ArgumentDefinition<>(new CommonIntegerArgument(field.getName(), min, max)));
    }

    private static List<ArgumentDefinition<?>> floatArguments(Field field) {
        Range range = field.getAnnotation(Range.class);
        float min = range == null ? -Float.MAX_VALUE : (float) range.min();
        float max = range == null ? Float.MAX_VALUE : (float) range.max();
        return List.of(new ArgumentDefinition<>(new CommonFloatArgument(field.getName(), min, max)));
    }

    private static List<ArgumentDefinition<?>> doubleArguments(Field field) {
        Range range = field.getAnnotation(Range.class);
        double min = range == null ? -Double.MAX_VALUE : range.min();
        double max = range == null ? Double.MAX_VALUE : range.max();
        return List.of(new ArgumentDefinition<>(new CommonDoubleArgument(field.getName(), min, max)));
    }

    private static List<ArgumentDefinition<?>> longArguments(Field field) {
        Range range = field.getAnnotation(Range.class);
        long min = range == null ? Long.MIN_VALUE : (long) Math.ceil(range.min());
        long max = range == null ? Long.MAX_VALUE : (long) Math.floor(range.max());
        return List.of(new ArgumentDefinition<>(new CommonLongArgument<>(field.getName(), min, max)));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static List<ArgumentDefinition<?>> enumArguments(Field field) {
        return List.of(new ArgumentDefinition<>(new CommonEnumArgument(field.getName(), wrap(field.getType()))));
    }

    private static final class ArgumentRule {
        private final Predicate<Class<?>> typePredicate;
        private final Function<Field, List<ArgumentDefinition<?>>> argumentFactory;

        private ArgumentRule(Predicate<Class<?>> typePredicate,
                             Function<Field, List<ArgumentDefinition<?>>> argumentFactory) {
            this.typePredicate = typePredicate;
            this.argumentFactory = argumentFactory;
        }

        private boolean supports(Class<?> type) {
            return typePredicate.test(type);
        }
    }
}
