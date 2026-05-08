package net.kunmc.lab.configlib;

import net.kunmc.lab.commandlib.CommandContext;
import net.kunmc.lab.commandlib.CommonCommandContext;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;

public final class ConfigCommandDescriptions {
    private static final Map<Key, String> ENGLISH = createEnglish();
    private static final Map<Key, String> JAPANESE = createJapanese();
    private static final Provider DEFAULT_PROVIDER = localized(Map.of("en",
                                                                      ENGLISH,
                                                                      "en_us",
                                                                      ENGLISH,
                                                                      "ja",
                                                                      JAPANESE,
                                                                      "ja_jp",
                                                                      JAPANESE));

    private ConfigCommandDescriptions() {
    }

    /**
     * Returns ConfigLib's built-in command text provider.
     * <p>
     * The default provider uses {@link CommandContext#getLanguage()} and supports English and Japanese.
     * Unsupported languages fall back to English.
     * </p>
     */
    public static Provider defaultProvider() {
        return DEFAULT_PROVIDER;
    }

    /**
     * Creates a provider backed by language-code keyed templates.
     * <p>
     * Language keys should use CommandLib language codes such as {@code en_us} and {@code ja_jp}.
     * A language-only key such as {@code ja} can also be used as a fallback for all Japanese variants.
     * Templates use named placeholders such as {@code {entry}} and {@code {max}}.
     * Prefix literal braces with a backslash.
     * </p>
     */
    public static Provider localized(@NotNull Map<String, Map<Key, String>> translations) {
        Objects.requireNonNull(translations);
        Map<String, Map<Key, String>> map = new HashMap<>();
        translations.forEach((language, values) -> {
            Objects.requireNonNull(language);
            Objects.requireNonNull(values);
            map.put(language, Collections.unmodifiableMap(new EnumMap<>(values)));
        });

        return (ctx, key, args) -> {
            String language = ctx.getLanguage();
            String languagePrefix = languagePrefixOf(language);

            String template = null;
            if (map.containsKey(language)) {
                template = map.get(language)
                              .get(key);
            } else if (map.containsKey(languagePrefix)) {
                template = map.get(languagePrefix)
                              .get(key);
            }
            if (template == null) {
                template = ENGLISH.get(key);
            }

            return render(template, args);
        };
    }

    public static Map<Key, String> english() {
        return ENGLISH;
    }

    public static Map<Key, String> japanese() {
        return JAPANESE;
    }

    public static Args args(Object... keyValues) {
        return Args.of(keyValues);
    }

    private static Map<Key, String> createEnglish() {
        EnumMap<Key, String> descriptions = new EnumMap<>(Key.class);
        descriptions.put(Key.ROOT, "Inspect and modify registered configs.");
        descriptions.put(Key.CONFIG, "Inspect and manage '{config}'.");
        descriptions.put(Key.LIST, "List current values.");
        descriptions.put(Key.RELOAD, "Reload configs from disk.");
        descriptions.put(Key.RELOAD_CONFIG, "Reload '{config}' from disk.");
        descriptions.put(Key.RESET, "Reset config values to their defaults.");
        descriptions.put(Key.RESET_CONFIG, "Reset all values in '{config}' to their defaults.");
        descriptions.put(Key.HISTORY, "Browse restorable config snapshots.");
        descriptions.put(Key.HISTORY_CONFIG, "Browse history for '{config}'.");
        descriptions.put(Key.HISTORY_INDEX, "Show a history entry. 0 is the latest snapshot.");
        descriptions.put(Key.HISTORY_DIFF, "Compare history snapshots.");
        descriptions.put(Key.AUDIT, "Browse the change audit log.");
        descriptions.put(Key.AUDIT_CONFIG, "Browse audit entries for '{config}'.");
        descriptions.put(Key.AUDIT_INDEX, "Show an audit entry in detail. 0 is the newest entry.");
        descriptions.put(Key.UNDO, "Restore a previous history snapshot.");
        descriptions.put(Key.UNDO_CONFIG, "Restore '{config}' from history.");
        descriptions.put(Key.UNDO_INDEX, "History index to restore. 1 restores the previous snapshot.");
        descriptions.put(Key.DIFF, "Show differences between defaults and history snapshots.");
        descriptions.put(Key.DIFF_CONFIG, "Show differences for '{config}'.");
        descriptions.put(Key.DIFF_DEFAULT, "Compare current values against defaults.");
        descriptions.put(Key.DIFF_INDEX, "Compare the current snapshot against the given history index.");
        descriptions.put(Key.DIFF_INDEX_PAIR, "Compare two history indexes. 0 is current, larger values are older.");
        descriptions.put(Key.FIELD_GET_MODIFY, "Show or modify '{entry}'.");
        descriptions.put(Key.FIELD_GET, "Show '{entry}'.");
        descriptions.put(Key.FIELD_MODIFY, "Modify '{entry}'.");
        descriptions.put(Key.SET, "Set '{entry}' to a new value.");
        descriptions.put(Key.RESET_ENTRY, "Reset '{entry}' to its default value.");
        descriptions.put(Key.ADD, "Add element(s) to '{entry}'.");
        descriptions.put(Key.REMOVE, "Remove element(s) from '{entry}'.");
        descriptions.put(Key.CLEAR, "Clear all values from '{entry}'.");
        descriptions.put(Key.INCREMENT, "Increase '{entry}'.");
        descriptions.put(Key.INCREMENT_BY, "Increase '{entry}' by the given amount.");
        descriptions.put(Key.DECREMENT, "Decrease '{entry}'.");
        descriptions.put(Key.DECREMENT_BY, "Decrease '{entry}' by the given amount.");
        descriptions.put(Key.PUT, "Put or replace a key-value pair in '{entry}'.");
        descriptions.put(Key.REMOVE_MAP, "Remove a key-value pair from '{entry}'.");
        descriptions.put(Key.CLEAR_MAP, "Remove all key-value pairs from '{entry}'.");
        descriptions.put(Key.HISTORY_EMPTY, "{config} has no history entries.");
        descriptions.put(Key.HISTORY_INDEX_OUT_OF_RANGE, "Index {index} is out of range (0-{max}).");
        descriptions.put(Key.HISTORY_UNKNOWN_TIMESTAMP, "unknown");
        descriptions.put(Key.HISTORY_LATEST_SUFFIX, " (latest)");
        descriptions.put(Key.AUDIT_EMPTY, "{config} has no audit entries.");
        descriptions.put(Key.AUDIT_REASON_LABEL, "reason");
        descriptions.put(Key.AUDIT_ACTOR_LABEL, "actor");
        descriptions.put(Key.AUDIT_PATHS_LABEL, "paths");
        descriptions.put(Key.AUDIT_ACTOR_SUFFIX, " by {actor}");
        descriptions.put(Key.INDEX_OUT_OF_RANGE, "Index {index} is out of range (0-{max}).");
        descriptions.put(Key.DIFF_SAME_INDEX, "Cannot diff the same index.");
        descriptions.put(Key.DIFF_NOT_ENOUGH_HISTORY, "Not enough history entries for {config}.");
        descriptions.put(Key.DIFF_DEFAULT_LABEL, "default");
        descriptions.put(Key.DIFF_CURRENT_LABEL, "current");
        descriptions.put(Key.DIFF_NONE, "No differences found.");
        descriptions.put(Key.RELOAD_SUCCESS, "{config} was reloaded.");
        descriptions.put(Key.RELOAD_FAILURE, "Failed to reload {config}.");
        descriptions.put(Key.RESET_SUCCESS, "{config} was reset to default values.");
        descriptions.put(Key.UNDO_NO_RESTORABLE_HISTORY, "No restorable history entries for {config}.");
        descriptions.put(Key.UNDO_SUCCESS, "{config} restored history[{index}].");
        descriptions.put(Key.UNDO_NOT_RESTORABLE, "History index {index} is not restorable for {config}.");
        descriptions.put(Key.FIELD_RESET_SUCCESS, "{entry} was reset to default ({value}).");
        descriptions.put(Key.SINGLE_VALUE_MODIFY_SUCCESS, "{entry} was changed to {value}.");
        descriptions.put(Key.COLLECTION_ADD_SUCCESS, "{entry} added {value}.");
        descriptions.put(Key.COLLECTION_REMOVE_SUCCESS, "{entry} removed {value}.");
        descriptions.put(Key.COLLECTION_CLEAR_SUCCESS, "{entry} was cleared.");
        descriptions.put(Key.MAP_PUT_SUCCESS, "{entry} put \\{{key}:{value}\\}.");
        descriptions.put(Key.MAP_REMOVE_SUCCESS, "{entry} removed \\{{key}:{value}\\}.");
        descriptions.put(Key.MAP_CLEAR_SUCCESS, "{entry} was cleared.");
        descriptions.put(Key.VALIDATION_FAILED, "Validation failed for {path} (value: {value}).");
        descriptions.put(Key.INVALID_VALUE, "Invalid value.");
        descriptions.put(Key.VALUE_NOT_NULL, "Value must not be null.");
        descriptions.put(Key.NUMERIC_RANGE, "Enter a value between {min} and {max}.");
        descriptions.put(Key.POJO_NOT_NULL, "{field} must not be null.");
        descriptions.put(Key.POJO_RANGE_NON_NUMERIC, "@Range can only be used on numeric fields: {field}.");
        descriptions.put(Key.POJO_RANGE, "{field} must be between {min} and {max}.");
        descriptions.put(Key.COMMAND_NOT_EXECUTABLE, "This command cannot be executed.");
        return Collections.unmodifiableMap(descriptions);
    }

    private static Map<Key, String> createJapanese() {
        EnumMap<Key, String> descriptions = new EnumMap<>(Key.class);
        descriptions.put(Key.ROOT, "設定を表示・変更します。");
        descriptions.put(Key.CONFIG, "'{config}' の設定を表示・管理します。");
        descriptions.put(Key.LIST, "現在の設定値を一覧表示します。");
        descriptions.put(Key.RELOAD, "設定をファイルから再読み込みします。");
        descriptions.put(Key.RELOAD_CONFIG, "'{config}' をファイルから再読み込みします。");
        descriptions.put(Key.RESET, "設定値をデフォルト値に戻します。");
        descriptions.put(Key.RESET_CONFIG, "'{config}' のすべての値をデフォルト値に戻します。");
        descriptions.put(Key.HISTORY, "復元可能な設定スナップショットを表示します。");
        descriptions.put(Key.HISTORY_CONFIG, "'{config}' の履歴を表示します。");
        descriptions.put(Key.HISTORY_INDEX, "履歴エントリを詳細表示します。0 が最新のスナップショットです。");
        descriptions.put(Key.HISTORY_DIFF, "履歴スナップショット同士を比較します。");
        descriptions.put(Key.AUDIT, "変更監査ログを表示します。");
        descriptions.put(Key.AUDIT_CONFIG, "'{config}' の監査ログを表示します。");
        descriptions.put(Key.AUDIT_INDEX, "監査エントリを詳細表示します。0 が最新のエントリです。");
        descriptions.put(Key.UNDO, "過去の履歴スナップショットへ復元します。");
        descriptions.put(Key.UNDO_CONFIG, "'{config}' を履歴から復元します。");
        descriptions.put(Key.UNDO_INDEX, "復元する履歴indexです。1 が直前のスナップショットです。");
        descriptions.put(Key.DIFF, "デフォルト値や履歴スナップショットとの差分を表示します。");
        descriptions.put(Key.DIFF_CONFIG, "'{config}' の差分を表示します。");
        descriptions.put(Key.DIFF_DEFAULT, "現在値とデフォルト値を比較します。");
        descriptions.put(Key.DIFF_INDEX, "現在のスナップショットと指定した履歴indexを比較します。");
        descriptions.put(Key.DIFF_INDEX_PAIR, "2つの履歴indexを比較します。0 が現在で、数字が大きいほど古い履歴です。");
        descriptions.put(Key.FIELD_GET_MODIFY, "'{entry}' を表示または変更します。");
        descriptions.put(Key.FIELD_GET, "'{entry}' を表示します。");
        descriptions.put(Key.FIELD_MODIFY, "'{entry}' を変更します。");
        descriptions.put(Key.SET, "'{entry}' を新しい値に設定します。");
        descriptions.put(Key.RESET_ENTRY, "'{entry}' をデフォルト値に戻します。");
        descriptions.put(Key.ADD, "'{entry}' に要素を追加します。");
        descriptions.put(Key.REMOVE, "'{entry}' から要素を削除します。");
        descriptions.put(Key.CLEAR, "'{entry}' のすべての値を削除します。");
        descriptions.put(Key.INCREMENT, "'{entry}' を増加させます。");
        descriptions.put(Key.INCREMENT_BY, "'{entry}' を指定した量だけ増加させます。");
        descriptions.put(Key.DECREMENT, "'{entry}' を減少させます。");
        descriptions.put(Key.DECREMENT_BY, "'{entry}' を指定した量だけ減少させます。");
        descriptions.put(Key.PUT, "'{entry}' にキーと値のペアを追加または置換します。");
        descriptions.put(Key.REMOVE_MAP, "'{entry}' からキーと値のペアを削除します。");
        descriptions.put(Key.CLEAR_MAP, "'{entry}' のすべてのキーと値のペアを削除します。");
        descriptions.put(Key.HISTORY_EMPTY, "{config} の変更履歴はありません。");
        descriptions.put(Key.HISTORY_INDEX_OUT_OF_RANGE, "index {index} は範囲外です (0-{max})。");
        descriptions.put(Key.HISTORY_UNKNOWN_TIMESTAMP, "不明");
        descriptions.put(Key.HISTORY_LATEST_SUFFIX, " (最新)");
        descriptions.put(Key.AUDIT_EMPTY, "{config} の監査ログはありません。");
        descriptions.put(Key.AUDIT_REASON_LABEL, "理由");
        descriptions.put(Key.AUDIT_ACTOR_LABEL, "実行者");
        descriptions.put(Key.AUDIT_PATHS_LABEL, "対象");
        descriptions.put(Key.AUDIT_ACTOR_SUFFIX, " 実行者:{actor}");
        descriptions.put(Key.INDEX_OUT_OF_RANGE, "index {index} は範囲外です (0-{max})。");
        descriptions.put(Key.DIFF_SAME_INDEX, "同じindexは比較できません。");
        descriptions.put(Key.DIFF_NOT_ENOUGH_HISTORY, "{config} の履歴が不足しています。");
        descriptions.put(Key.DIFF_DEFAULT_LABEL, "デフォルト");
        descriptions.put(Key.DIFF_CURRENT_LABEL, "現在");
        descriptions.put(Key.DIFF_NONE, "差分はありません。");
        descriptions.put(Key.RELOAD_SUCCESS, "{config} を再読み込みしました。");
        descriptions.put(Key.RELOAD_FAILURE, "{config} の読み込みに失敗しました。");
        descriptions.put(Key.RESET_SUCCESS, "{config} をデフォルト値に戻しました。");
        descriptions.put(Key.UNDO_NO_RESTORABLE_HISTORY, "{config} に復元可能な履歴はありません。");
        descriptions.put(Key.UNDO_SUCCESS, "{config} を history[{index}] に復元しました。");
        descriptions.put(Key.UNDO_NOT_RESTORABLE, "履歴index {index} は {config} では復元できません。");
        descriptions.put(Key.FIELD_RESET_SUCCESS, "{entry} をデフォルト値に戻しました ({value})。");
        descriptions.put(Key.SINGLE_VALUE_MODIFY_SUCCESS, "{entry} の値を {value} に変更しました。");
        descriptions.put(Key.COLLECTION_ADD_SUCCESS, "{entry} に {value} を追加しました。");
        descriptions.put(Key.COLLECTION_REMOVE_SUCCESS, "{entry} から {value} を削除しました。");
        descriptions.put(Key.COLLECTION_CLEAR_SUCCESS, "{entry} をクリアしました。");
        descriptions.put(Key.MAP_PUT_SUCCESS, "{entry} に \\{{key}:{value}\\} を追加しました。");
        descriptions.put(Key.MAP_REMOVE_SUCCESS, "{entry} から \\{{key}:{value}\\} を削除しました。");
        descriptions.put(Key.MAP_CLEAR_SUCCESS, "{entry} をクリアしました。");
        descriptions.put(Key.VALIDATION_FAILED, "{path} の検証に失敗しました (value: {value})。");
        descriptions.put(Key.INVALID_VALUE, "不正な値です。");
        descriptions.put(Key.VALUE_NOT_NULL, "値をnullにできません。");
        descriptions.put(Key.NUMERIC_RANGE, "{min}以上{max}以下の値を入力してください。");
        descriptions.put(Key.POJO_NOT_NULL, "{field} はnullにできません。");
        descriptions.put(Key.POJO_RANGE_NON_NUMERIC, "@Range は数値フィールドにのみ使用できます: {field}。");
        descriptions.put(Key.POJO_RANGE, "{field} は {min} 以上 {max} 以下である必要があります。");
        descriptions.put(Key.COMMAND_NOT_EXECUTABLE, "このコマンドを実行できません。");
        return Collections.unmodifiableMap(descriptions);
    }

    public static String describe(@NotNull CommonCommandContext<?, ?> ctx, @NotNull Key key, Object... args) {
        return DEFAULT_PROVIDER.describe(ctx, key, args);
    }

    public static String describe(@NotNull CommonCommandContext<?, ?> ctx, @NotNull Key key, Args args) {
        return DEFAULT_PROVIDER.describe(ctx, key, args);
    }

    static Function<CommandContext, String> root(Provider provider) {
        return ctx -> provider.describe(ctx, Key.ROOT);
    }

    static Function<CommandContext, String> config(Provider provider, String configName) {
        return ctx -> provider.describe(ctx, Key.CONFIG, configName);
    }

    static Function<CommandContext, String> list(Provider provider) {
        return ctx -> provider.describe(ctx, Key.LIST);
    }

    static Function<CommandContext, String> reload(Provider provider) {
        return ctx -> provider.describe(ctx, Key.RELOAD);
    }

    static Function<CommandContext, String> reloadConfig(Provider provider, String configName) {
        return ctx -> provider.describe(ctx, Key.RELOAD_CONFIG, configName);
    }

    static Function<CommandContext, String> reset(Provider provider) {
        return ctx -> provider.describe(ctx, Key.RESET);
    }

    static Function<CommandContext, String> resetConfig(Provider provider, String configName) {
        return ctx -> provider.describe(ctx, Key.RESET_CONFIG, configName);
    }

    static Function<CommandContext, String> history(Provider provider) {
        return ctx -> provider.describe(ctx, Key.HISTORY);
    }

    static Function<CommandContext, String> historyConfig(Provider provider, String configName) {
        return ctx -> provider.describe(ctx, Key.HISTORY_CONFIG, configName);
    }

    static Function<CommandContext, String> historyIndex(Provider provider) {
        return ctx -> provider.describe(ctx, Key.HISTORY_INDEX);
    }

    static Function<CommandContext, String> historyDiff(Provider provider) {
        return ctx -> provider.describe(ctx, Key.HISTORY_DIFF);
    }

    static Function<CommandContext, String> audit(Provider provider) {
        return ctx -> provider.describe(ctx, Key.AUDIT);
    }

    static Function<CommandContext, String> auditConfig(Provider provider, String configName) {
        return ctx -> provider.describe(ctx, Key.AUDIT_CONFIG, configName);
    }

    static Function<CommandContext, String> auditIndex(Provider provider) {
        return ctx -> provider.describe(ctx, Key.AUDIT_INDEX);
    }

    static Function<CommandContext, String> undo(Provider provider) {
        return ctx -> provider.describe(ctx, Key.UNDO);
    }

    static Function<CommandContext, String> undoConfig(Provider provider, String configName) {
        return ctx -> provider.describe(ctx, Key.UNDO_CONFIG, configName);
    }

    static Function<CommandContext, String> undoIndex(Provider provider) {
        return ctx -> provider.describe(ctx, Key.UNDO_INDEX);
    }

    static Function<CommandContext, String> diff(Provider provider) {
        return ctx -> provider.describe(ctx, Key.DIFF);
    }

    static Function<CommandContext, String> diffConfig(Provider provider, String configName) {
        return ctx -> provider.describe(ctx, Key.DIFF_CONFIG, configName);
    }

    static Function<CommandContext, String> diffDefault(Provider provider) {
        return ctx -> provider.describe(ctx, Key.DIFF_DEFAULT);
    }

    static Function<CommandContext, String> diffIndex(Provider provider) {
        return ctx -> provider.describe(ctx, Key.DIFF_INDEX);
    }

    static Function<CommandContext, String> diffIndexPair(Provider provider) {
        return ctx -> provider.describe(ctx, Key.DIFF_INDEX_PAIR);
    }

    static Function<CommandContext, String> field(Provider provider,
                                                  String entryName,
                                                  boolean getEnabled,
                                                  boolean modifyEnabled) {
        if (getEnabled && modifyEnabled) {
            return ctx -> provider.describe(ctx, Key.FIELD_GET_MODIFY, entryName);
        }
        if (getEnabled) {
            return ctx -> provider.describe(ctx, Key.FIELD_GET, entryName);
        }
        return ctx -> provider.describe(ctx, Key.FIELD_MODIFY, entryName);
    }

    static Function<CommandContext, String> set(Provider provider, String entryName) {
        return ctx -> provider.describe(ctx, Key.SET, entryName);
    }

    static Function<CommandContext, String> resetEntry(Provider provider, String entryName) {
        return ctx -> provider.describe(ctx, Key.RESET_ENTRY, entryName);
    }

    static Function<CommandContext, String> add(Provider provider, String entryName) {
        return ctx -> provider.describe(ctx, Key.ADD, entryName);
    }

    static Function<CommandContext, String> remove(Provider provider, String entryName) {
        return ctx -> provider.describe(ctx, Key.REMOVE, entryName);
    }

    static Function<CommandContext, String> clear(Provider provider, String entryName) {
        return ctx -> provider.describe(ctx, Key.CLEAR, entryName);
    }

    static Function<CommandContext, String> increment(Provider provider, String entryName) {
        return ctx -> provider.describe(ctx, Key.INCREMENT, entryName);
    }

    static Function<CommandContext, String> incrementBy(Provider provider, String entryName) {
        return ctx -> provider.describe(ctx, Key.INCREMENT_BY, entryName);
    }

    static Function<CommandContext, String> decrement(Provider provider, String entryName) {
        return ctx -> provider.describe(ctx, Key.DECREMENT, entryName);
    }

    static Function<CommandContext, String> decrementBy(Provider provider, String entryName) {
        return ctx -> provider.describe(ctx, Key.DECREMENT_BY, entryName);
    }

    static Function<CommandContext, String> put(Provider provider, String entryName) {
        return ctx -> provider.describe(ctx, Key.PUT, entryName);
    }

    static Function<CommandContext, String> removeMap(Provider provider, String entryName) {
        return ctx -> provider.describe(ctx, Key.REMOVE_MAP, entryName);
    }

    static Function<CommandContext, String> clearMap(Provider provider, String entryName) {
        return ctx -> provider.describe(ctx, Key.CLEAR_MAP, entryName);
    }

    private static String languagePrefixOf(String language) {
        int separator = language.indexOf('_');
        if (separator < 0) {
            return language;
        }
        return language.substring(0, separator);
    }

    private static Args argsFor(Key key, Object... args) {
        switch (key) {
            case CONFIG:
            case RELOAD_CONFIG:
            case RESET_CONFIG:
            case HISTORY_CONFIG:
            case AUDIT_CONFIG:
            case UNDO_CONFIG:
            case DIFF_CONFIG:
            case HISTORY_EMPTY:
            case AUDIT_EMPTY:
            case DIFF_NOT_ENOUGH_HISTORY:
            case RELOAD_SUCCESS:
            case RELOAD_FAILURE:
            case RESET_SUCCESS:
            case UNDO_NO_RESTORABLE_HISTORY:
                return Args.of("config", valueAt(args, 0));
            case FIELD_GET_MODIFY:
            case FIELD_GET:
            case FIELD_MODIFY:
            case SET:
            case RESET_ENTRY:
            case ADD:
            case REMOVE:
            case CLEAR:
            case INCREMENT:
            case INCREMENT_BY:
            case DECREMENT:
            case DECREMENT_BY:
            case PUT:
            case REMOVE_MAP:
            case CLEAR_MAP:
            case COLLECTION_CLEAR_SUCCESS:
            case MAP_CLEAR_SUCCESS:
                return Args.of("entry", valueAt(args, 0));
            case HISTORY_INDEX_OUT_OF_RANGE:
            case INDEX_OUT_OF_RANGE:
                return Args.of("index", valueAt(args, 0), "max", valueAt(args, 1));
            case UNDO_SUCCESS:
                return Args.of("config", valueAt(args, 0), "index", valueAt(args, 1));
            case UNDO_NOT_RESTORABLE:
                return Args.of("index", valueAt(args, 0), "config", valueAt(args, 1));
            case FIELD_RESET_SUCCESS:
            case SINGLE_VALUE_MODIFY_SUCCESS:
            case COLLECTION_ADD_SUCCESS:
            case COLLECTION_REMOVE_SUCCESS:
                return Args.of("entry", valueAt(args, 0), "value", valueAt(args, 1));
            case MAP_PUT_SUCCESS:
            case MAP_REMOVE_SUCCESS:
                return Args.of("entry", valueAt(args, 0), "key", valueAt(args, 1), "value", valueAt(args, 2));
            case VALIDATION_FAILED:
                return Args.of("path", valueAt(args, 0), "value", valueAt(args, 1));
            case NUMERIC_RANGE:
                return Args.of("min", valueAt(args, 0), "max", valueAt(args, 1));
            case POJO_NOT_NULL:
            case POJO_RANGE_NON_NUMERIC:
                return Args.of("field", valueAt(args, 0));
            case POJO_RANGE:
                return Args.of("field", valueAt(args, 0), "min", valueAt(args, 1), "max", valueAt(args, 2));
            case AUDIT_ACTOR_SUFFIX:
                return Args.of("actor", valueAt(args, 0));
            default:
                return Args.positional(args);
        }
    }

    private static Object valueAt(Object[] args, int index) {
        if (args == null || index >= args.length) {
            return null;
        }
        return args[index];
    }

    private static String render(String template, Args args) {
        if (template == null || template.indexOf('{') < 0) {
            return template;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < template.length(); i++) {
            char c = template.charAt(i);
            if (c == '\\' && i + 1 < template.length()) {
                char next = template.charAt(i + 1);
                if (next == '{' || next == '}') {
                    sb.append(next);
                    i++;
                    continue;
                }
            }
            if (c != '{') {
                sb.append(c);
                continue;
            }

            int end = template.indexOf('}', i + 1);
            if (end < 0) {
                sb.append(c);
                continue;
            }

            String name = template.substring(i + 1, end);
            if (name.isEmpty()) {
                sb.append("{}");
                i = end;
                continue;
            }
            if (!args.contains(name)) {
                throw new IllegalArgumentException("Missing template argument {" + name + "} for: " + template);
            }
            sb.append(args.get(name));
            i = end;
        }
        return sb.toString();
    }

    @FunctionalInterface
    public interface Provider {
        /**
         * Returns the command text for the given key and named arguments.
         */
        String describe(@NotNull CommonCommandContext<?, ?> ctx, @NotNull Key key, @NotNull Args args);

        default String describe(@NotNull CommonCommandContext<?, ?> ctx, @NotNull Key key, Object... args) {
            return describe(ctx, key, argsFor(key, args));
        }
    }

    public static final class Args {
        private final Map<String, Object> values;

        private Args(Map<String, Object> values) {
            this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
        }

        /**
         * Creates named template arguments from key-value pairs.
         * <p>
         * Values may be null. Null values are rendered as {@code "null"}.
         * </p>
         */
        public static Args of(Object... keyValues) {
            if (keyValues.length % 2 != 0) {
                throw new IllegalArgumentException("keyValues must contain key-value pairs.");
            }

            Map<String, Object> values = new LinkedHashMap<>();
            for (int i = 0; i < keyValues.length; i += 2) {
                Object key = keyValues[i];
                if (!(key instanceof String)) {
                    throw new IllegalArgumentException("Template argument key must be a String: " + key);
                }
                values.put((String) key, keyValues[i + 1]);
            }
            return new Args(values);
        }

        private static Args positional(Object... args) {
            Map<String, Object> values = new LinkedHashMap<>();
            for (int i = 0; args != null && i < args.length; i++) {
                values.put(String.valueOf(i), args[i]);
            }
            return new Args(values);
        }

        public Object get(String key) {
            return values.get(key);
        }

        public boolean contains(String key) {
            return values.containsKey(key);
        }

        public Map<String, Object> asMap() {
            return values;
        }
    }

    public enum Key {
        ROOT,
        CONFIG,
        LIST,
        RELOAD,
        RELOAD_CONFIG,
        RESET,
        RESET_CONFIG,
        HISTORY,
        HISTORY_CONFIG,
        HISTORY_INDEX,
        HISTORY_DIFF,
        AUDIT,
        AUDIT_CONFIG,
        AUDIT_INDEX,
        UNDO,
        UNDO_CONFIG,
        UNDO_INDEX,
        DIFF,
        DIFF_CONFIG,
        DIFF_DEFAULT,
        DIFF_INDEX,
        DIFF_INDEX_PAIR,
        FIELD_GET_MODIFY,
        FIELD_GET,
        FIELD_MODIFY,
        SET,
        RESET_ENTRY,
        ADD,
        REMOVE,
        CLEAR,
        INCREMENT,
        INCREMENT_BY,
        DECREMENT,
        DECREMENT_BY,
        PUT,
        REMOVE_MAP,
        CLEAR_MAP,
        HISTORY_EMPTY,
        HISTORY_INDEX_OUT_OF_RANGE,
        HISTORY_UNKNOWN_TIMESTAMP,
        HISTORY_LATEST_SUFFIX,
        AUDIT_EMPTY,
        AUDIT_REASON_LABEL,
        AUDIT_ACTOR_LABEL,
        AUDIT_PATHS_LABEL,
        AUDIT_ACTOR_SUFFIX,
        INDEX_OUT_OF_RANGE,
        DIFF_SAME_INDEX,
        DIFF_NOT_ENOUGH_HISTORY,
        DIFF_DEFAULT_LABEL,
        DIFF_CURRENT_LABEL,
        DIFF_NONE,
        RELOAD_SUCCESS,
        RELOAD_FAILURE,
        RESET_SUCCESS,
        UNDO_NO_RESTORABLE_HISTORY,
        UNDO_SUCCESS,
        UNDO_NOT_RESTORABLE,
        FIELD_RESET_SUCCESS,
        SINGLE_VALUE_MODIFY_SUCCESS,
        COLLECTION_ADD_SUCCESS,
        COLLECTION_REMOVE_SUCCESS,
        COLLECTION_CLEAR_SUCCESS,
        MAP_PUT_SUCCESS,
        MAP_REMOVE_SUCCESS,
        MAP_CLEAR_SUCCESS,
        VALIDATION_FAILED,
        INVALID_VALUE,
        VALUE_NOT_NULL,
        NUMERIC_RANGE,
        POJO_NOT_NULL,
        POJO_RANGE_NON_NUMERIC,
        POJO_RANGE,
        COMMAND_NOT_EXECUTABLE
    }
}
