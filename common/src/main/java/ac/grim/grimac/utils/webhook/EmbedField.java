package ac.grim.grimac.utils.webhook;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

@Getter
@Setter
@Accessors(fluent = true)
public class EmbedField implements JsonSerializable {
    public static final int MAX_NAME_LENGTH = 256;
    public static final int MAX_VALUE_LENGTH = 1024;

    private @NotNull String name;
    private @NotNull String value;
    private boolean inline;

    public EmbedField(@NotNull String name, @NotNull String value) {
        this(name, value, false);
    }

    public EmbedField(@NotNull String name, @NotNull String value, boolean inline) {
        name(name);
        value(value);
        inline(inline);
    }

    public EmbedField(@NotNull JsonElement jsonElement) {
        JsonObject json = jsonElement.getAsJsonObject();
        name(json.get("name").getAsString());
        value(json.get("value").getAsString());
        JsonElement inline = json.get("inline");
        if (inline != null) inline(inline.getAsBoolean());
    }

    @Contract(value = "_ -> this", mutates = "this")
    public @NotNull EmbedField name(@NotNull String name) {
        requireNonNull(name, "Embed field name cannot be null!");
        if (name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Embed field name too long, " + name.length() + " > " + MAX_NAME_LENGTH);
        }

        this.name = name;
        return this;
    }

    @Contract(value = "_ -> this", mutates = "this")
    public @NotNull EmbedField value(@NotNull String value) {
        requireNonNull(value, "Embed field value cannot be null!");
        if (value.length() > MAX_VALUE_LENGTH) {
            throw new IllegalArgumentException("Embed field value too long, " + value.length() + " > " + MAX_VALUE_LENGTH);
        }

        this.value = value;
        return this;
    }

    @Override
    public @NotNull JsonElement toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("name", name());
        json.addProperty("value", value());
        json.addProperty("inline", inline());
        return json;
    }
}
