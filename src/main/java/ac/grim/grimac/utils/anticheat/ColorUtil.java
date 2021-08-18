package ac.grim.grimac.utils.anticheat;

import lombok.experimental.UtilityClass;
import org.bukkit.ChatColor;

@UtilityClass
public class ColorUtil {
    // & to paragraph symbol
    public String format(final String string) {
        return ChatColor.translateAlternateColorCodes('&', string);
    }
}
