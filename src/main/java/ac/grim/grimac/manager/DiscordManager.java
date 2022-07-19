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

public class DiscordManager implements Initable {
    private static WebhookClient client;
    private int embedColor;
    private String staticContent = "";

    @Override
    public void start() {
        try {
            if (!GrimAPI.INSTANCE.getConfigManager().getConfig().getBooleanElse("enabled", false)) return;

            client = WebhookClient.withUrl(GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("webhook", ""));
            if (client.getUrl().isEmpty()) {
                LogUtil.warn("Discord webhook is empty, disabling Discord alerts");
                client = null;
                return;
            }
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
