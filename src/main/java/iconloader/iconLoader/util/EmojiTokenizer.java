package iconloader.iconLoader.util;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Turns the configured {@code chat.token-format} into the regex every feature shares, so chat,
 * the anvil listener, tab completion and the GUI all agree on what an emoji token looks like.
 */
public final class EmojiTokenizer {
    public static final String DEFAULT_FORMAT = ":{name}:";
    private static final String NAME_PLACEHOLDER = "{name}";
    // Deliberately excludes ':' so a greedy match can never swallow two adjacent tokens.
    private static final String NAME_PATTERN = "([A-Za-z0-9_<+>-]+)";

    private static final Map<String, Pattern> COMPILED = new ConcurrentHashMap<>();

    private EmojiTokenizer() {}

    public static String sanitizeFormat(String configured) {
        if (configured == null || configured.isBlank() || !configured.contains(NAME_PLACEHOLDER)) {
            return DEFAULT_FORMAT;
        }
        return configured;
    }

    public static String renderToken(String glyphName, String configuredFormat) {
        return sanitizeFormat(configuredFormat).replace(NAME_PLACEHOLDER, glyphName);
    }

    public static Pattern pattern(FileConfiguration config) {
        String format = sanitizeFormat(config.getString("chat.token-format", DEFAULT_FORMAT));
        boolean caseInsensitive = config.getBoolean("chat.case-insensitive", true);
        return COMPILED.computeIfAbsent(format + "|" + caseInsensitive, key -> compile(format, caseInsensitive));
    }

    private static Pattern compile(String format, boolean caseInsensitive) {
        StringBuilder regex = new StringBuilder();
        int cursor = 0;
        while (true) {
            int placeholder = format.indexOf(NAME_PLACEHOLDER, cursor);
            if (placeholder < 0) {
                regex.append(Pattern.quote(format.substring(cursor)));
                break;
            }
            regex.append(Pattern.quote(format.substring(cursor, placeholder)));
            regex.append(NAME_PATTERN);
            cursor = placeholder + NAME_PLACEHOLDER.length();
        }
        return Pattern.compile(regex.toString(), caseInsensitive ? Pattern.CASE_INSENSITIVE : 0);
    }

    public static String lowercase(String value) {
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }
}
