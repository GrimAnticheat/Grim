package ac.grim.grimac.manager;

import ac.grim.grimac.manager.init.Initable;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.math.GrimMath;
import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import io.github.retrooper.packetevents.PacketEvents;
import org.bukkit.configuration.file.FileConfiguration;

import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DiscordManager implements Initable {
    private static WebhookClient client;

    @Override
    public void start() {
        try {
            FileConfiguration config = ConfigManager.getDiscordConfig();

            if (!config.getBoolean("enabled", false)) return;

            client = WebhookClient.withUrl(config.getString("webhook", ""));
            client.setTimeout(15000); // Requests expire after 15 seconds

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendAlert(GrimPlayer player, String checkName, String violations, String verbose) {
        if (client != null) {

            String tps = String.format("%.2f", PacketEvents.get().getServerUtils().getTPS());
            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String formattedPing = "" + GrimMath.floor(player.getTransactionPing() / 1e6);

            String ver = player.getClientVersion().name();
            if (ver.startsWith("v")) ver = ver.substring(2);
            ver = ver.replace("_", ".");
            String formattedVer = ver;

            String content = "**Player**\n" + player.bukkitPlayer.getName()
                    + "\n**Check**\n" + checkName
                    + "\n**Violations**\n " + violations
                    + "\n**Client Version**\n" + formattedVer
                    + "\n**Ping**\n" + formattedPing
                    + "\n**TPS**\n" + tps;

            WebhookEmbedBuilder embed = new WebhookEmbedBuilder()
                    .setImageUrl("https://i.stack.imgur.com/Fzh0w.png") // Constant width
                    .setColor(Color.CYAN.getRGB())
                    // Discord caches this for around 24 hours, this is abuse of neither CraftHead nor discord
                    .setThumbnailUrl("https://crafthead.net/avatar/" + player.bukkitPlayer.getUniqueId())
                    .setTitle(new WebhookEmbed.EmbedTitle("**Grim Alert**", null))
                    .setDescription(content)
                    .setFooter(new WebhookEmbed.EmbedFooter(time, "https://grim.ac/images/grim.png"));

            if (!verbose.isEmpty()) {
                embed.addField(new WebhookEmbed.EmbedField(true, "Verbose", verbose));
            }

            try {
                client.send(embed.build());
            } catch (Exception ignored) {
            }
        }
    }
}
