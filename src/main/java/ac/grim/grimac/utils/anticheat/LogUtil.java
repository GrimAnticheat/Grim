package ac.grim.grimac.utils.anticheat;

import ac.grim.grimac.GrimAPI;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.util.logging.Logger;

@UtilityClass
public class LogUtil {
    public void info(final String info) {
        getLogger().info(info);
    }

    public void warn(final String warn) {
        getLogger().warning(warn);
    }

    public void error(final String error) {
        getLogger().severe(error);
    }

    public Logger getLogger() {
        return GrimAPI.INSTANCE.getPlugin().getLogger();
    }

    public void console(final String info) {
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', info));
    }

    public void console(final Component info) {
        MessageUtil.sendMessage(Bukkit.getConsoleSender(), info);
    }
}
