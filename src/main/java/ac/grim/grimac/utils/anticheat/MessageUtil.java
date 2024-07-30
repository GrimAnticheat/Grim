package ac.grim.grimac.utils.anticheat;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import lombok.experimental.UtilityClass;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public class MessageUtil {
    private final BukkitAudiences adventure = BukkitAudiences.create(GrimAPI.INSTANCE.getPlugin());
    private final boolean hasPlaceholderAPI;

    static {
        boolean placeholderAPI;

        try {
            Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            placeholderAPI = true;
        } catch (ClassNotFoundException ignored) {
            placeholderAPI = false;
        }

        hasPlaceholderAPI = placeholderAPI;
    }

    public @NotNull String replacePlaceholders(@NotNull GrimPlayer player, @NotNull String string) {
        return replacePlaceholders(player.bukkitPlayer, GrimAPI.INSTANCE.getExternalAPI().replaceVariables(player, string));
    }

    public @NotNull String replacePlaceholders(@Nullable Object object, @NotNull String string) {
        if (!hasPlaceholderAPI) return string;

        OfflinePlayer player = null;
        if (object instanceof OfflinePlayer) {
            player = (OfflinePlayer) object;
        }

        return PlaceholderAPI.setPlaceholders(player, string);
    }

    public @NotNull String replacePlaceholders(@Nullable OfflinePlayer player, @NotNull String string) {
        return hasPlaceholderAPI ? PlaceholderAPI.setPlaceholders(player, string) : string;
    }

    public @NotNull Component miniMessage(@NotNull String string) {
        string = string.replace("%prefix%", GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("prefix", "&bGrim &8»"));

        // hex codes
        if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_16)) {
            Matcher matcher = Pattern.compile("&#[A-Fa-f0-9]{6}").matcher(string);
            StringBuffer sb = new StringBuffer(string.length());

            while (matcher.find()) {
                matcher.appendReplacement(sb, "<#" + matcher.group(1) + ">");
            }

            matcher.appendTail(sb);
            string = sb.toString();
        }

        // MiniMessage doesn't like legacy formatting codes
        string = ChatColor.translateAlternateColorCodes('&', string)
                .replace("§0", "<black>")
                .replace("§1", "<dark_blue>")
                .replace("§2", "<dark_green>")
                .replace("§3", "<dark_aqua>")
                .replace("§4", "<dark_red>")
                .replace("§5", "<dark_purple>")
                .replace("§6", "<gold>")
                .replace("§7", "<gray>")
                .replace("§8", "<dark_gray>")
                .replace("§9", "<blue>")
                .replace("§a", "<green>")
                .replace("§b", "<aqua>")
                .replace("§c", "<red>")
                .replace("§d", "<light_purple>")
                .replace("§e", "<yellow>")
                .replace("§f", "<white>")
                .replace("§r", "<reset>")
                .replace("§k", "<obfuscated>")
                .replace("§l", "<bold>")
                .replace("§m", "<strikethrough>")
                .replace("§n", "<underlined>")
                .replace("§o", "<italic>");

        return MiniMessage.miniMessage().deserialize(string).compact();
    }

    public void sendMessage(CommandSender commandSender, @NotNull Component component) {
        adventure.sender(commandSender).sendMessage(component);
    }
}
