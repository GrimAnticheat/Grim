package ac.grim.grimac.utils.webhook;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Objects;

import static ac.grim.grimac.utils.webhook.JsonSerializable.deserializeArray;
import static ac.grim.grimac.utils.webhook.JsonSerializable.serializeArray;
import static java.util.Objects.requireNonNull;

@Getter
@Setter
@Accessors(fluent = true)
public class Embed implements JsonSerializable {
    public static final int MAX_TITLE_LENGTH = 256;
    public static final int MAX_DESCRIPTION_LENGTH = 4096;
    public static final int MAX_FIELDS = 25;

    private @Nullable String title;
    private @NotNull String description;
    private @Nullable String titleURL;
    private @Nullable Instant timestamp;
    private @Nullable Integer color;
    private @Nullable EmbedFooter footer;
    private @Nullable String imageURL;
    private @Nullable String thumbnailURL;
    private @Nullable EmbedAuthor author;
    private @NotNull EmbedField @Nullable [] fields;

    public Embed(@NotNull String description) {
        description(description);
    }

    public Embed(@NotNull JsonElement jsonElement) {
        JsonObject json = jsonElement.getAsJsonObject();
        description(json.get("description").getAsString());

        JsonElement element;
        if ((element = json.get("title")) != null) title(element.getAsString());
        if ((element = json.get("url")) != null) titleURL(element.getAsString());
        if ((element = json.get("timestamp")) != null) timestamp(Instant.parse(element.getAsString()));
        if ((element = json.get("color")) != null) color(element.getAsInt());
        if ((element = json.get("footer")) != null) footer(new EmbedFooter(element));
        if ((element = json.get("image")) != null) imageURL(element.getAsJsonObject().get("url").getAsString());
        if ((element = json.get("thumbnail")) != null) imageURL(element.getAsJsonObject().get("url").getAsString());
        if ((element = json.get("author")) != null) author(new EmbedAuthor(element));
        if ((element = json.get("fields")) != null) fields(deserializeArray(element.getAsJsonArray(), EmbedField[]::new, EmbedField::new));
    }

    @Contract(value = "_ -> this", mutates = "this")
    public @NotNull Embed description(@NotNull String description) {
        requireNonNull(description, "Embed description cannot be null!");
        if (description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("Embed description too long, " + description.length() + " > " + MAX_DESCRIPTION_LENGTH);
        }

        this.description = description;
        return this;
    }

    @Contract(value = "_ -> this", mutates = "this")
    public @NotNull Embed title(@Nullable String title) {
        if (title != null && title.length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException("Embed title too long, " + title.length() + " > " + MAX_TITLE_LENGTH);
        }

        this.title = title;
        return this;
    }

    @Contract(value = "_ -> this", mutates = "this")
    public @NotNull Embed fields(@NotNull EmbedField @Nullable [] fields) {
        if (fields != null) {
            if (fields.length > MAX_FIELDS) {
                throw new IllegalArgumentException("Too many fields, " + fields.length + " > " + MAX_FIELDS);
            }

            for (EmbedField field : fields) {
                Objects.requireNonNull(field);
            }
        }

        this.fields = fields;
        return this;
    }

    @Contract(value = "_ -> this", mutates = "this")
    public @NotNull Embed addFields(@NotNull EmbedField @NotNull ... fields) {
        if (fields.length == 0) return this;
        if (fields() == null) return fields(fields);

        EmbedField[] newFields = new EmbedField[fields().length + fields.length];

        System.arraycopy(fields(), 0, newFields, 0, fields().length);
        System.arraycopy(fields, fields().length, newFields, fields().length, fields.length);

        return fields(newFields);
    }

    public @NotNull Embed footer(@Nullable EmbedFooter footer) {
        if (footer == null || footer.icon() == null && footer.text().isBlank()) {
            this.footer = null;
        } else {
            this.footer = footer;
        }
        return this;
    }

    public @NotNull JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("description", description());
        if (title() != null) json.addProperty("title", title());
        if (color() != null) json.addProperty("color", color() & 0xffffff);
        if (titleURL() != null) json.addProperty("url", titleURL());
        if (timestamp() != null) json.addProperty("timestamp", timestamp().toString());
        if (footer() != null) json.add("footer", footer().toJson());
        if (imageURL() != null) {
            JsonObject image = new JsonObject();
            image.addProperty("url", imageURL());
            json.add("image", image);
        }
        if (thumbnailURL() != null) {
            JsonObject thumbnail = new JsonObject();
            thumbnail.addProperty("url", thumbnailURL());
            json.add("thumbnail", thumbnail);
        }
        if (author() != null) json.add("author", author().toJson());
        if (fields() != null) json.add("fields", serializeArray(fields()));
        return json;
    }
}
