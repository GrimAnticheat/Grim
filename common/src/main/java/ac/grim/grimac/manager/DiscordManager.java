package ac.grim.grimac.manager;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.manager.init.ReloadableInitable;
import ac.grim.grimac.manager.init.start.StartableInitable;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import ac.grim.grimac.utils.webhook.Embed;
import ac.grim.grimac.utils.webhook.EmbedField;
import ac.grim.grimac.utils.webhook.EmbedFooter;
import ac.grim.grimac.utils.webhook.WebhookMessage;

import java.awt.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class DiscordManager implements StartableInitable, ReloadableInitable {
    public static final Predicate<String> WEBHOOK_REGEX = Pattern.compile("https://discord.com/api/webhooks/\\d+/[\\w-]+").asMatchPredicate();
    public static final Duration timeout = Duration.ofSeconds(15);
    private static final HttpClient client = HttpClient.newBuilder().connectTimeout(timeout).build();
    private static final ConcurrentLinkedDeque<HttpRequest> requests = new ConcurrentLinkedDeque<>();
    private static final AtomicBoolean taskStarted = new AtomicBoolean();
    private static final AtomicBoolean sending = new AtomicBoolean();
    private static long rateLimitedUntil;
    private URI url;
    private int embedColor;
    private String staticContent = "";
    private String embedTitle = "";

    @Override
    public void start() {
        reload();
    }

    @Override
    public void reload() {
        try {
            if (!GrimAPI.INSTANCE.getConfigManager().getConfig().getBooleanElse("enabled", false)) {
                url = null;
                return;
            }

            String webhook = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("webhook", "");

            if (!WEBHOOK_REGEX.test(webhook)) {
                LogUtil.error("Discord webhook url does not follow expected format (https://discord.com/api/webhooks/<id>/<token>): " + webhook);
                url = null;
            } else {
                url = new URI(webhook);
            }

            embedTitle = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("embed-title", "**Grim Alert**");

            try {
                embedColor = Color.decode(GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("embed-color", "#00FFFF")).getRGB();
            } catch (NumberFormatException e) {
                LogUtil.warn("Discord embed color is invalid");
            }

            StringBuilder sb = new StringBuilder();
            for (String string : GrimAPI.INSTANCE.getConfigManager().getConfig().getStringListElse("violation-content", getDefaultContents())) {
                sb.append(string).append("\n");
            }
            staticContent = sb.toString();
        } catch (Exception e) {
            LogUtil.error("Failed to load Discord webhook configuration", e);
        }
    }

    private List<String> getDefaultContents() {
        List<String> list = new ArrayList<>();
        list.add("**Player**: %player%");
        list.add("**Check**: %check%");
        list.add("**Violations**: %violations%");
        list.add("**Client Version**: %version%");
        list.add("**Brand**: %brand%");
        list.add("**Ping**: %ping%");
        list.add("**TPS**: %tps%");
        return list;
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
                .imageURL("https://i.stack.imgur.com/Fzh0w.png")
                .thumbnailURL("https://crafthead.net/helm/" + player.user.getProfile().getUUID())
                .color(embedColor)
                .title(embedTitle)
                .timestamp(Instant.now())
                .footer(new EmbedFooter("", "https://grim.ac/images/grim.png"));

        if (!verbose.isEmpty()) {
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
