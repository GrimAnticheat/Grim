package ac.grim.grimac.platform.bukkit.initables;

import ac.grim.grimac.manager.init.OptionalReflectiveInitable;
import org.bukkit.Bukkit;

public final class BukkitLuckPermsInitable extends OptionalReflectiveInitable {
    private static final String HANDLER_CLASS =
            "ac.grim.grimac.platform.bukkit.initables.BukkitLuckPermsHandler";

    public BukkitLuckPermsInitable() {
        super(HANDLER_CLASS, "Error when initializing LuckPerms hook");
    }

    @Override
    protected boolean isAvailable() {
        return Bukkit.getPluginManager().getPlugin("LuckPerms") != null;
    }
}
