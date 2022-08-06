package ac.grim.grimac.manager;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.manager.init.Initable;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.LogUtil;
import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;

import java.awt.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DiscordManager implements Initable {
    private static WebhookClient client;
    private int embedColor;
    private String staticContent = "";

    public static final Pattern WEBHOOK_PATTERN = Pattern.compile("(?:https?://)?(?:\\w+\\.)?\\w+\\.\\w+/api(?:/v\\d+)?/webhooks/(\\d+)/([\\w-]+)(?:/(?:\\w+)?)?");

    @Override
    public void start() {
        try {
            if (!GrimAPI.INSTANCE.getConfigManager().getConfig().getBooleanElse("enabled", false)) return;
            String webhook = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("webhook", "");
            if (webhook.isEmpty()) {
                LogUtil.warn("Discord webhook is empty, disabling Discord alerts");
                client = null;
                return;
            }
            //
            Matcher matcher = WEBHOOK_PATTERN.matcher(webhook);
            if (!matcher.matches()) {
                throw new IllegalArgumentException("Failed to parse webhook URL");
            }
            client = WebhookClient.withId(Long.parseUnsignedLong(matcher.group(1)), matcher.group(2));
            client.setTimeout(15000); // Requests expire after 15 seconds

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
            e.printStackTrace();
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

    public void sendAlert(GrimPlayer player, String verbose, String checkName, String violations) {
        if (client != null) {

            String content = staticContent + "";
            content = content.replace("%check%", checkName);
            content = content.replace("%violations%", violations);
            content = GrimAPI.INSTANCE.getExternalAPI().replaceVariables(player, content, false);
            content = content.replace("_", "\\_");

            WebhookEmbedBuilder embed = new WebhookEmbedBuilder()
                    .setImageUrl("https://i.stack.imgur.com/Fzh0w.png") // Constant width
                    .setThumbnailUrl("https://crafthead.net/helm/" + player.user.getProfile().getUUID())
                    .setColor(embedColor)
                    .setTitle(new WebhookEmbed.EmbedTitle("**Grim Alert**", null))
                    .setDescription(content)
                    .setTimestamp(Instant.now())
                    .setFooter(new WebhookEmbed.EmbedFooter("", "https://grim.ac/images/grim.png"));

            if (!verbose.isEmpty()) {
                embed.addField(new WebhookEmbed.EmbedField(true, "Verbose", verbose));
            }

            sendWebhookEmbed(embed);
        }
    }

    public void sendWebhookEmbed(WebhookEmbedBuilder embed) {
        try {
            client.send(embed.build());
        } catch (Exception ignored) {
        }
    }
}
