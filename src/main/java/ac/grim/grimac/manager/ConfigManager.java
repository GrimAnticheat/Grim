package ac.grim.grimac.manager;

import ac.grim.grimac.GrimAPI;
import lombok.experimental.UtilityClass;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

@UtilityClass
public class ConfigManager {

    public String getPrefix() {
        return getConfig().getString("prefix", "&bGrimAC &f»");
    }

    public FileConfiguration getConfig() {
        return GrimAPI.INSTANCE.getPlugin().getConfig();
    }

    public YamlConfiguration getDiscordConfig() {
        try {
            File discord = new File(GrimAPI.INSTANCE.getPlugin().getDataFolder(), "discord_en.yml");
            YamlConfiguration config = new YamlConfiguration();
            config.load(discord);
            return config;
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }

        return null;
    }
}
