package ac.grim.grimac.manager.init.start;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.manager.init.Initable;
import org.bukkit.plugin.Plugin;

public class DefaultConfigGenerator implements Initable {
    @Override
    public void start() {
        Plugin grim = GrimAPI.INSTANCE.getPlugin();

        grim.saveDefaultConfig();
        grim.reloadConfig();

        grim.saveResource("discord.yml", false);


    }
}
