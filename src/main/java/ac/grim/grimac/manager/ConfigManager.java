package ac.grim.grimac.manager;

import ac.grim.grimac.GrimAPI;
import lombok.experimental.UtilityClass;
import org.bukkit.configuration.file.FileConfiguration;

@UtilityClass
public class ConfigManager {

    public String getPrefix() {
        return getConfig().getString("prefix", "&bGrimAC &f»");
    }

    public FileConfiguration getConfig() {
        return GrimAPI.INSTANCE.getPlugin().getConfig();
    }
}
