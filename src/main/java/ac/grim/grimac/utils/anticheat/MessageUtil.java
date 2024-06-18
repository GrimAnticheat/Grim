package ac.grim.grimac.utils.anticheat;

import ac.grim.grimac.GrimAPI;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import lombok.experimental.UtilityClass;
import net.md_5.bungee.api.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public class MessageUtil {

    public String format(String string) {
        string = formatWithNoColor(string);
        if(PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_16))
            string = translateHexCodes(string);
        return ChatColor.translateAlternateColorCodes('&', string);
    }

    public String formatWithNoColor(String string) {
        return string.replace("%prefix%", GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("prefix", "&bGrim &8»"));
    }

    private String translateHexCodes(String message) {
        final String hexPattern = "#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{8})";
        Matcher matcher = Pattern.compile(hexPattern).matcher(message);
        StringBuffer sb = new StringBuffer(message.length());
        while (matcher.find()) {
            String hex = matcher.group(1);
            ChatColor color = ChatColor.of("#" + hex);
            matcher.appendReplacement(sb, color.toString());
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
