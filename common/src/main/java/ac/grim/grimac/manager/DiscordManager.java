package ac.grim.grimac.manager;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.manager.init.ReloadableInitable;
import ac.grim.grimac.manager.init.start.StartableInitable;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import ac.grim.grimac.utils.webhook.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.awt.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class DiscordManager implements StartableInitable, ReloadableInitable {
    private static final Predicate<String> WEBHOOK_REGEX = Pattern.compile("^https://discord\\.com/api(?:/v\\d+)?/webhooks/\\d+/[\\w-]+(\\?thread_id=\\d+)?$").asMatchPredicate();
    private static final Duration timeout = Duration.ofSeconds(15);
    private static final HttpClient client = HttpClient.newBuilder().connectTimeout(timeout).build();
    private static final ConcurrentLinkedDeque<HttpRequest> requests = new ConcurrentLinkedDeque<>();
    private static final AtomicBoolean taskStarted = new AtomicBoolean();
    private static final AtomicBoolean sending = new AtomicBoolean();
    private static long rateLimitedUntil;
    private URI url;
    private int embedColor;
    private String staticContent = "";
    private String embedTitle = "";
    private boolean includeTimestamp;
    private boolean includeVerbose;
    private @Nullable String embedImageUrl;
    private @Nullable String embedThumbnailUrl;
    private @Nullable String embedFooterUrl;
    private String embedFooterText = "";

    private static final Pattern URL_PATTERN = Pattern.compile("^https?://(?:www\\.)?[-a-z0-9@:%._+~#=]{1,256}\\.[a-z0-9()]{1,6}\\b[-a-z0-9()@:%_+.~#?&/=]*$", Pattern.CASE_INSENSITIVE);

    private static String validatedConfigURL(String configPath, String defaultURL) {
        String url = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("embed-image-url", defaultURL);
        if (url == null || url.isBlank()) return null;
        if (URL_PATTERN.matcher(url).matches()) {
            return url;
        } else {
            LogUtil.warn("Invalid embed url for config path " + configPath + ": " + configPath);
            return defaultURL;
        }
    }

    @Override
    public void start() {
        reload();
    }

    @Override
    public void reload() {
        try {
            ConfigManager config = GrimAPI.INSTANCE.getConfigManager().getConfig();
            if (!config.getBooleanElse("enabled", false)) {
                url = null;
                return;
            }

            String webhook = config.getStringElse("webhook", "");

            if (!WEBHOOK_REGEX.test(webhook)) {
                LogUtil.error("Discord webhook url does not follow expected format (https://discord.com/api/webhooks/<id>/<token>): " + webhook);
                url = null;
            } else {
                url = new URI(webhook);
            }
            // not adding these to the config since they may change in the future
            // mainly for just for allowing more customization
            embedImageUrl = validatedConfigURL("embed-image-url", null);
            embedThumbnailUrl = validatedConfigURL("embed-thumbnail-url", "https://crafthead.net/helm/%uuid%");
            embedFooterUrl = validatedConfigURL("embed-footer-url", "https://grim.ac/images/grim.png");
            embedFooterText = config.getStringElse("embed-footer-text", "v%grim_version%");
            embedTitle = config.getStringElse("embed-title", "**Grim Alert**");

            try {
                embedColor = Color.decode(config.getStringElse("embed-color", "#00FFFF")).getRGB();
            } catch (NumberFormatException e) {
                LogUtil.warn("Discord embed color is invalid");
            }

            StringBuilder sb = new StringBuilder();
            for (String string : config.getStringListElse("violation-content", getDefaultContents())) {
                sb.append(string).append("\n");
            }
            staticContent = sb.toString();
            includeTimestamp = config.getBooleanElse("include-timestamp", true);
            includeVerbose = config.getBooleanElse("include-verbose", true);
        } catch (Exception e) {
            LogUtil.error("Failed to load Discord webhook configuration", e);
        }
    }

    @Contract(value = " -> new", pure = true)
    private @NotNull @Unmodifiable List<@NotNull String> getDefaultContents() {
        return List.of(
                "**Player**: %player%",
                "**Check**: %check%",
                "**Violations**: %violations%",
                "**Client Version**: %version%",
                "**Brand**: %brand%",
                "**Ping**: %ping%",
                "**TPS**: %tps%"
        );
    }

    public void sendAlert(GrimPlayer player, String verbose, String checkName, int violations) {
        if (url == null) {
            return;
        }

        String content = staticContent;
        content = content.replace("%check%", checkName.replace("_", "\\_")); // just in case any checks are added with an underscore
        content = content.replace("%violations%", Integer.toString(violations));
        content = MessageUtil.replacePlaceholders(player, content, true);

        Embed embed = new Embed(content)
                .color(embedColor)
                .title(embedTitle)
                .imageURL(MessageUtil.replacePlaceholders(player, embedImageUrl, false))
                .thumbnailURL(MessageUtil.replacePlaceholders(player, embedThumbnailUrl, false))
                .footer(new EmbedFooter(
                        MessageUtil.replacePlaceholders(player, embedFooterText, true),
                        MessageUtil.replacePlaceholders(player, embedFooterUrl, false)
                ));

        if (includeTimestamp) embed.timestamp(Instant.now());

        if (!verbose.isEmpty() && includeVerbose) {
            embed.addFields(new EmbedField("Verbose", MessageUtil.filterDiscordText(verbose), true));
        }

        sendWebhookMessage(new WebhookMessage().addEmbeds(embed));
    }

    public void sendWebhookMessage(WebhookMessage message) {
        requests.add(HttpRequest.newBuilder()
                .uri(url)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(message.toJson().toString()))
                .timeout(timeout)
                .build());

        if (!taskStarted.getAndSet(true)) {
            // there's probably a better way to handle rate limits, but this works, so whatever.
            GrimAPI.INSTANCE.getScheduler().getAsyncScheduler().runAtFixedRate(GrimAPI.INSTANCE.getGrimPlugin(), DiscordManager::tick, 0, 1);
        }
    }

    private static void tick() {
        HttpRequest request = requests.peek();
        if (request != null && rateLimitedUntil < System.currentTimeMillis() && !sending.getAndSet(true)) {
            client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).whenComplete((response, throwable) -> {
                if (throwable != null) {
                    sending.set(false);
                    LogUtil.error("Exception caught while sending a Discord webhook alert", throwable);
                    return;
                }

                if (response != null && response.statusCode() == 429) {
                    sending.set(false);
                    rateLimitedUntil = Math.max(response.headers().firstValueAsLong("X-RateLimit-Reset").getAsLong() * 1000, rateLimitedUntil);
                    return;
                }

                requests.remove(request);
                sending.set(false);

                // TODO: handle 503 (Service Unavailable)?
                if (response != null && response.statusCode() >= 400) {
                    LogUtil.error("Encountered status code " + response.statusCode() + " with body " + response.body() + " and headers " + response.headers().map() + " while sending a Discord webhook alert.");
                }
            });
        }
    }
}
