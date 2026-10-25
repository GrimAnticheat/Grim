package ac.grim.grimac.utils.common;

import org.incendo.cloud.suggestion.Suggestion;

import java.util.List;

/** Helpers for values passed through Brigadier string arguments. */
public final class BrigadierArgumentUtil {

    private BrigadierArgumentUtil() {
    }

    /**
     * Removes one opening quote so a quoted argument can be used as a suggestion lookup prefix.
     */
    public static String stripOpeningQuote(String value) {
        if (value.isEmpty()) return value;
        char first = value.charAt(0);
        return first == '"' || first == '\'' ? value.substring(1) : value;
    }

    /**
     * Quotes a value when Brigadier's unquoted word parser would reject one of its characters.
     * Values that already fit the word grammar are returned unchanged.
     */
    public static String quoteIfNeeded(String value) {
        boolean word = !value.isEmpty();
        for (int i = 0; word && i < value.length(); i++) {
            word = isWordCharacter(value.charAt(i));
        }
        if (word) return value;

        StringBuilder quoted = new StringBuilder(value.length() + 2);
        quoted.append('"');
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if (character == '"') quoted.append('\\');
            quoted.append(character);
        }
        return quoted.append('"').toString();
    }

    /** Returns a suggestion quoted when Brigadier cannot parse it as an unquoted word. */
    public static Suggestion quoteSuggestion(Suggestion suggestion) {
        String value = suggestion.suggestion();
        String quoted = quoteIfNeeded(value);
        return quoted.equals(value) ? suggestion : suggestion.withSuggestion(quoted);
    }

    /** Quotes the entries in place, preserving suggestion metadata for ordinary values. */
    public static void quoteSuggestions(List<Suggestion> suggestions) {
        for (int i = 0; i < suggestions.size(); i++) {
            suggestions.set(i, quoteSuggestion(suggestions.get(i)));
        }
    }

    private static boolean isWordCharacter(char character) {
        return Character.isLetterOrDigit(character)
                || character == '_'
                || character == '-'
                || character == '.'
                || character == '+';
    }
}
