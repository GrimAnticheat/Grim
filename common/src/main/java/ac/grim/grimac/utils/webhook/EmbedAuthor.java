package ac.grim.grimac.utils.webhook;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.util.Objects.requireNonNull;

@Getter
@Setter
@Accessors(fluent = true)
public class EmbedAuthor implements JsonSerializable {
    public static final int MAX_NAME_LENGTH = 256;

    private @NotNull String name;
    private @Nullable String url;
    private @Nullable String icon;

    public EmbedAuthor(@NotNull String name) {
        this(name, null, null);
    }

    public EmbedAuthor(@NotNull String name, @Nullable String url, @Nullable String icon) {
        name(name);
        url(url);
        icon(icon);
    }

    public EmbedAuthor(@NotNull JsonElement jsonElement) {
        JsonObject json = jsonElement.getAsJsonObject();
        name(json.get("name").getAsString());

        JsonElement element;
        if ((element = json.get("url")) != null) url(element.getAsString());
        if ((element = json.get("icon_url")) != null) icon(element.getAsString());
    }

    @Contract(value = "_ -> this", mutates = "this")
    public @NotNull EmbedAuthor name(@NotNull String name) {
        requireNonNull(name, "Embed author name cannot be null!");
        if (name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Embed author name too long, " + name.length() + " > " + MAX_NAME_LENGTH);
        }

        this.name = name;
        return this;
    }

    @Override
    public @NotNull JsonElement toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("name", name());
        if (url() != null) json.addProperty("url", url());
        if (icon() != null) json.addProperty("icon_url", icon());
        return json;
    }
}
