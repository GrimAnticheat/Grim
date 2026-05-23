package ac.grim.grimac.platform.fabric.utils.metrics;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * An extremely simple JSON builder.
 *
 * <p>While this class is neither feature-rich nor the most performant one, it's sufficient enough
 * for its use-case.
 */
public class JsonObjectBuilder {

    private StringBuilder builder = new StringBuilder();

    private boolean hasAtLeastOneField = false;

    public JsonObjectBuilder() {
        builder.append("{");
    }

    /**
     * Escapes the given string like stated in <a href="https://www.ietf.org/rfc/rfc4627.txt">https://www.ietf.org/rfc/rfc4627.txt</a>.
     *
     * <p>This method escapes only the necessary characters '"', '\'. and '\u0000' - '\u001F'.
     * Compact escapes are not used (e.g., '\n' is escaped as "\u000a" and not as "\n").
     *
     * @param value The value to escape.
     * @return The escaped value.
     */
    private static String escape(String value) {
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '"') {
                builder.append("\\\"");
            } else if (c == '\\') {
                builder.append("\\\\");
            } else if (c <= '\u000F') {
                builder.append("\\u000").append(Integer.toHexString(c));
            } else if (c <= '\u001F') {
                builder.append("\\u00").append(Integer.toHexString(c));
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }

    /**
     * Appends a null field to the JSON.
     *
     * @param key The key of the field.
     * @return A reference to this object.
     */
    public JsonObjectBuilder appendNull(String key) {
        appendFieldUnescaped(key, "null");
        return this;
    }

    /**
     * Appends a string field to the JSON.
     *
     * @param key   The key of the field.
     * @param value The value of the field.
     * @return A reference to this object.
     */
    public JsonObjectBuilder appendField(String key, String value) {
        if (value == null) {
            throw new IllegalArgumentException("JSON value must not be null");
        }
        appendFieldUnescaped(key, "\"" + escape(value) + "\"");
        return this;
    }

    /**
     * Appends an integer field to the JSON.
     *
     * @param key   The key of the field.
     * @param value The value of the field.
     * @return A reference to this object.
     */
    public JsonObjectBuilder appendField(String key, int value) {
        appendFieldUnescaped(key, String.valueOf(value));
        return this;
    }

    /**
     * Appends an object to the JSON.
     *
     * @param key    The key of the field.
     * @param object The object.
     * @return A reference to this object.
     */
    public JsonObjectBuilder appendField(String key, JsonObject object) {
        if (object == null) {
            throw new IllegalArgumentException("JSON object must not be null");
        }
        appendFieldUnescaped(key, object.toString());
        return this;
    }

    /**
     * Appends a string array to the JSON.
     *
     * @param key    The key of the field.
     * @param values The string array.
     * @return A reference to this object.
     */
    public JsonObjectBuilder appendField(String key, String[] values) {
        if (values == null) {
            throw new IllegalArgumentException("JSON values must not be null");
        }
        String escapedValues =
                Arrays.stream(values)
                        .map(value -> "\"" + escape(value) + "\"")
                        .collect(Collectors.joining(","));
        appendFieldUnescaped(key, "[" + escapedValues + "]");
        return this;
    }

    /**
     * Appends an integer array to the JSON.
     *
     * @param key    The key of the field.
     * @param values The integer array.
     * @return A reference to this object.
     */
    public JsonObjectBuilder appendField(String key, int[] values) {
        if (values == null) {
            throw new IllegalArgumentException("JSON values must not be null");
        }
        String escapedValues =
                Arrays.stream(values).mapToObj(String::valueOf).collect(Collectors.joining(","));
        appendFieldUnescaped(key, "[" + escapedValues + "]");
        return this;
    }

    /**
     * Appends an object array to the JSON.
     *
     * @param key    The key of the field.
     * @param values The integer array.
     * @return A reference to this object.
     */
    public JsonObjectBuilder appendField(String key, JsonObject[] values) {
        if (values == null) {
            throw new IllegalArgumentException("JSON values must not be null");
        }
        String escapedValues =
                Arrays.stream(values).map(JsonObject::toString).collect(Collectors.joining(","));
        appendFieldUnescaped(key, "[" + escapedValues + "]");
        return this;
    }

    /**
     * Appends a field to the object.
     *
     * @param key          The key of the field.
     * @param escapedValue The escaped value of the field.
     */
    private void appendFieldUnescaped(String key, String escapedValue) {
        if (builder == null) {
            throw new IllegalStateException("JSON has already been built");
        }
        if (key == null) {
            throw new IllegalArgumentException("JSON key must not be null");
        }
        if (hasAtLeastOneField) {
            builder.append(",");
        }
        builder.append("\"").append(escape(key)).append("\":").append(escapedValue);
        hasAtLeastOneField = true;
    }

    /**
     * Builds the JSON string and invalidates this builder.
     *
     * @return The built JSON string.
     */
    public JsonObject build() {
        if (builder == null) {
            throw new IllegalStateException("JSON has already been built");
        }
        JsonObject object = new JsonObject(builder.append("}").toString());
        builder = null;
        return object;
    }

    /**
     * A super simple representation of a JSON object.
     *
     * <p>This class only exists to make methods of the {@link JsonObjectBuilder} type-safe and not
     * allow a raw string inputs for methods like {@link JsonObjectBuilder#appendField(String,
     * JsonObject)}.
     */
    public static class JsonObject {

        private final String value;

        private JsonObject(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }
}
