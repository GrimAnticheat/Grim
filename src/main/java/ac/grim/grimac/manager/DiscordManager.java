package ac.grim.grimac.manager;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.checks.impl.misc.ClientBrand;
import ac.grim.grimac.manager.init.Initable;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.math.GrimMath;
import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import io.github.retrooper.packetevents.util.SpigotReflectionUtil;
import lombok.Setter;

import java.awt.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class DiscordManager implements Initable {
    private static WebhookClient client;
    private int embedColor;
    private String staticContent = "";

    @Setter
    private String serverName = "Unknown";

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
            String tps = String.format("%.2f", SpigotReflectionUtil.getTPS());
            String formattedPing = "" + GrimMath.floor(player.getTransactionPing() / 1e6);
            String formattedVer = player.getClientVersion().getReleaseName();
            String brand = player.checkManager.getPacketCheck(ClientBrand.class).getBrand().replace("_", "\\_");
            String name = (player.bukkitPlayer != null ? player.bukkitPlayer.getName() : player.user.getProfile().getName()).replace("_", "\\_");
            String uuidString = player.user.getProfile().getUUID().toString();

            String content = staticContent + "";
            content = content.replace("%uuid%", uuidString);
            content = content.replace("%player%", name);
            content = content.replace("%check%", checkName);
            content = content.replace("%violations%", violations);
            content = content.replace("%version%", formattedVer);
            content = content.replace("%brand%", brand);
            content = content.replace("%ping%", formattedPing);
            content = content.replace("%tps%", tps);
            content = content.replace("%server%", serverName);

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
