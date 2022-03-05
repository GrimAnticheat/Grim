package ac.grim.grimac.utils.anticheat;

import ac.grim.grimac.GrimAPI;
import lombok.experimental.UtilityClass;
import org.bukkit.ChatColor;

@UtilityClass
public class MessageUtil {
    // & to paragraph symbol
    public String format(String string) {
        string = string.replace("%prefix%", GrimAPI.INSTANCE.getPlugin().getConfig().getString("prefix", "&bGrim &8»"));
        return ChatColor.translateAlternateColorCodes('&', string);
    }
}
