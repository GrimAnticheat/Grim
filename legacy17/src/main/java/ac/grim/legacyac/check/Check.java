package ac.grim.legacyac.check;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.data.PlayerData;
import java.util.List;
import org.bukkit.entity.Player;

public abstract class Check {
    protected final LegacyAntiCheatPlugin plugin;
    private final String name;

    protected Check(LegacyAntiCheatPlugin plugin, String name) {
        this.plugin = plugin;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    protected boolean isEnabled() {
        return plugin.getConfig().getBoolean("checks." + name + ".enabled", true);
    }

    protected double getMaxViolation() {
        return plugin.getConfig().getDouble("checks." + name + ".max-vl", 10.0D);
    }

    protected boolean isExempt(Player player, PlayerData data) {
        return isExempt(player, data, false);
    }

    protected boolean isExempt(Player player, PlayerData data, boolean ignoreVelocityGrace) {
        if (player.hasPermission("grimlegacy.bypass")) {
            return true;
        }

        long now = System.currentTimeMillis();
        int joinGrace = plugin.getConfig().getInt("exempt.join-grace-ms", 2500);
        int teleportGrace = plugin.getConfig().getInt("exempt.teleport-grace-ms", 1000);
        int velocityGrace = plugin.getConfig().getInt("exempt.velocity-grace-ms", 900);

        if (now - data.getJoinAt() < joinGrace) {
            return true;
        }
        if (now - data.getLastTeleportAt() < teleportGrace) {
            return true;
        }
        if (!ignoreVelocityGrace && now - data.getLastVelocityAt() < velocityGrace) {
            return true;
        }

        return false;
    }

    protected double increaseBuffer(PlayerData data, double amount) {
        return data.addBuffer(name, amount);
    }

    protected void flag(Player player, PlayerData data, double amount, String detail) {
        if (!isEnabled() || isExempt(player, data)) {
            return;
        }

        double vl = data.addViolation(name, amount);
        plugin.alerts().alert(player, name, vl, detail);

        if (vl >= getMaxViolation() && plugin.getConfig().getBoolean("checks." + name + ".setback", true) && data.getLastSafeLocation() != null) {
            player.teleport(data.getLastSafeLocation());
        }

        runPunishments(player, data, vl);
    }

    private void runPunishments(Player player, PlayerData data, double vl) {
        double punishVl = plugin.getConfig().getDouble("checks." + name + ".punish-vl", -1.0D);
        if (punishVl <= 0.0D || vl < punishVl || data.hasExecutedPunish(name)) {
            return;
        }

        List<String> commands = plugin.getConfig().getStringList("checks." + name + ".punish-commands");
        for (String command : commands) {
            String parsed = command.replace("%player%", player.getName())
                .replace("%check%", name)
                .replace("%vl%", String.format("%.2f", vl));
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), parsed);
        }
        data.markPunishExecuted(name);
    }
}
