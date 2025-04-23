package ac.grim.grimac.utils.webhook;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static ac.grim.grimac.utils.webhook.JsonSerializable.deserializeArray;
import static ac.grim.grimac.utils.webhook.JsonSerializable.serializeArray;

@Getter
@Setter
@Accessors(fluent = true)
public class WebhookMessage implements JsonSerializable {
    public static final int MAX_CONTENT_LENGTH = 2000;
    public static final int MAX_EMBEDS = 10;

    private @Nullable String content;
    private @Nullable String username;
    private @Nullable String avatar;
    private @Nullable Boolean tts;
    private @NotNull Embed @Nullable [] embeds;

    public WebhookMessage() {}

    public WebhookMessage(@NotNull JsonObject json) {
        JsonElement element;
        if ((element = json.get("content")) != null) content(element.getAsString());
        if ((element = json.get("username")) != null) username(element.getAsString());
        if ((element = json.get("avatar_url")) != null) avatar(element.getAsString());
        if ((element = json.get("tts")) != null) tts(element.getAsBoolean());
        if ((element = json.get("embeds")) != null) embeds(deserializeArray(element.getAsJsonArray(), Embed[]::new, Embed::new));
    }

    @Contract(value = "_ -> this", mutates = "this")
    public @NotNull WebhookMessage content(@Nullable String content) {
        if (content != null && content.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("Webhook content too long, " + content.length() + " > " + MAX_CONTENT_LENGTH);
        }

        this.content = content;
        return this;
    }

    @Contract(value = "_ -> this", mutates = "this")
    public @NotNull WebhookMessage embeds(@NotNull Embed @Nullable [] embeds) {
        if (embeds != null) {
            if (embeds.length > MAX_EMBEDS) {
                throw new IllegalArgumentException("Too many embeds, " + embeds.length + " > " + MAX_EMBEDS);
            }

            for (Embed embed : embeds) {
                Objects.requireNonNull(embed);
            }
        }

        this.embeds = embeds;
        return this;
    }

    @Contract(value = "_ -> this", mutates = "this")
    public @NotNull WebhookMessage addEmbeds(@NotNull Embed @NotNull ... embeds) {
        if (embeds.length == 0) return this;
        if (embeds() == null) return embeds(embeds);

        Embed[] newEmbeds = new Embed[embeds().length + embeds.length];

        System.arraycopy(embeds(), 0, newEmbeds, 0, embeds().length);
        System.arraycopy(embeds, embeds().length, newEmbeds, embeds().length, embeds.length);

        return embeds(newEmbeds);
    }

    public @NotNull JsonObject toJson() {
        JsonObject json = new JsonObject();
        if (content() != null) json.addProperty("content", content());
        if (username() != null) json.addProperty("username", username());
        if (avatar() != null) json.addProperty("avatar_url", avatar());
        if (tts() != null) json.addProperty("tts", tts());
        if (embeds() != null) json.add("embeds", serializeArray(embeds()));
        return json;
    }
}
