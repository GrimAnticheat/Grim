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
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DiscordManager implements Initable {
    private static WebhookClient client;
    private int embedColor;
    private String staticContent = "";
    // Custom fields - not ideal but it works + it's easy to configure
    private List<Map<String, Object>> customFields;

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

            // never tested what would happen if fields is empty
            customFields = new ArrayList<>();
            List<Map<String, Object>> fieldMaps = GrimAPI.INSTANCE.getConfigManager().getConfig().getListElse("webhook.fields", getDefaultFields());
            customFields.addAll(fieldMaps);
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

    public List<Map<String, Object>> getDefaultFields() {
        List<Map<String, Object>> fields = new ArrayList<>();

        // Server Information field
        Map<String, Object> serverInfoField = new HashMap<>();
        serverInfoField.put("name", "Server Information");
        serverInfoField.put("value", Arrays.asList(
                "```properties",
                "Server: %server%",
                "TPS: %tps%",
                "```"
        ));
        serverInfoField.put("inline", false);
        fields.add(serverInfoField);

        // Client Information field
        Map<String, Object> clientInfoField = new HashMap<>();
        clientInfoField.put("name", "Client Information");
        clientInfoField.put("value", Arrays.asList(
                "```properties",
                "Version: %version%",
                "Brand: %brand%",
                "```"
        ));
        clientInfoField.put("inline", false);
        fields.add(clientInfoField);

        // Player Information field
        Map<String, Object> playerInfoField = new HashMap<>();
        playerInfoField.put("name", "Player Information");
        playerInfoField.put("value", Arrays.asList(
                "```properties",
                "Player: %player%",
                "UUID: %uuid%",
                "Ping: %ping%",
                "Horizontal Sensitivity: %h_sensitivity%%",
                "Vertical Sensitivity: %v_sensitivity%%",
                "Fast Math: %fast_math%",
                "```"
        ));
        playerInfoField.put("inline", false);
        fields.add(playerInfoField);

        // Check Information field
        Map<String, Object> checkInfoField = new HashMap<>();
        checkInfoField.put("name", "Check Information");
        checkInfoField.put("value", Arrays.asList(
                "```properties",
                "Check: %check%",
                "Violations: %violations%",
                "```"
        ));
        checkInfoField.put("inline", false);
        fields.add(checkInfoField);

        return fields;
    }

    public void sendAlert(GrimPlayer player, String verbose, String checkName, String violations) {
        if (client != null) {

            String description = staticContent;
            description = description.replace("%check%", checkName);
            description = description.replace("%violations%", violations);
            description = description.replace("%grim_version%", GrimAPI.INSTANCE.getPlugin().getDescription().getVersion());
            description = GrimAPI.INSTANCE.getExternalAPI().replaceVariables(player, description, false);
            description = description.replaceAll("_", "\\_");

            WebhookEmbedBuilder embed = new WebhookEmbedBuilder()
                    .setImageUrl("https://i.stack.imgur.com/Fzh0w.png") // Constant width
                    .setColor(embedColor)
                    .setTitle(new WebhookEmbed.EmbedTitle("**Grim Alert**", null))
                    .setDescription(description)
                    .setTimestamp(Instant.now())
                    .setFooter(new WebhookEmbed.EmbedFooter("", "https://grim.ac/images/grim.png"));

            if (GrimAPI.INSTANCE.getConfigManager().getConfig().getBooleanElse("webhook.show-player-head", true)) {
                embed.setThumbnailUrl("https://crafthead.net/helm/" + player.user.getProfile().getUUID());
            }

            // Add custom fields if they exist
            for (Map<String, Object> field : customFields) {
                String name = (String) field.get("name");
                List<String> value = (List<String>) field.get("value");
                boolean inline = (boolean) field.getOrDefault("inline", false);

                // Replace placeholders in field values
                List<String> fieldValue = new ArrayList<>();
                for (String line : value) {
                    line = line.replace("%check%", checkName); // Replace %check% placeholder
                    line = line.replace("%violations%", violations); // Replace %violations% placeholder
                    line = line.replace("%grim_version%", GrimAPI.INSTANCE.getPlugin().getDescription().getVersion());
                    line = GrimAPI.INSTANCE.getExternalAPI().replaceVariables(player, line, false);
                    line = line.replaceAll("_", "\\_");
                    fieldValue.add(line);
                }

                StringBuilder fieldValueString = new StringBuilder();
                for (String line : fieldValue) {
                    fieldValueString.append(line).append("\n");
                }

                embed.addField(new WebhookEmbed.EmbedField(inline, name, fieldValueString.toString()));
            }

            if (!verbose.isEmpty()) {
                embed.addField(new WebhookEmbed.EmbedField(
                        true,
                        "Verbose",
                        "```properties\n" +
                                verbose +
                                "```"
                ));
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
