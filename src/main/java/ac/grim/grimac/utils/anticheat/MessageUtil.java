package ac.grim.grimac.utils.anticheat;

import ac.grim.grimac.GrimAPI;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public class MessageUtil {
    private final BukkitAudiences adventure = BukkitAudiences.create(GrimAPI.INSTANCE.getPlugin());

    public String format(String string) {
        string = formatWithNoColor(string);
        if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_16))
            string = translateHexCodes(string);
        return ChatColor.translateAlternateColorCodes('&', string);
    }

    public String formatWithNoColor(String string) {
        return string.replace("%prefix%", GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("prefix", "&bGrim &8»"));
    }

    public @NotNull Component miniMessage(String string) {
        string = formatWithNoColor(string);

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

    private String translateHexCodes(String message) {
        Matcher matcher = Pattern.compile("&#[A-Fa-f0-9]{6}").matcher(message);
        StringBuffer sb = new StringBuffer(message.length());
        while (matcher.find()) {
            matcher.appendReplacement(sb, ChatColor.of("#" + matcher.group(1)).toString());
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
