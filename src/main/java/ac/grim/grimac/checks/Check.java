package ac.grim.grimac.checks;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.ColorUtil;
import ac.grim.grimac.utils.math.GrimMath;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

// Class from https://github.com/Tecnio/AntiCheatBase/blob/master/src/main/java/me/tecnio/anticheat/check/Check.java
@Getter
public class Check<T> {
    protected final GrimPlayer player;
    public double violations;
    public double decay;
    public double setbackVL;
    public double alertVL;
    public int alertInterval;
    public int alertCount;
    public boolean secretTestServerVLStyle = false;
    private double buffer;
    private double maxBuffer;
    private double setback;
    private double flagCooldown;
    private double vlMultiplier;
    private String checkName;
    private String configName;
    private long reset;

    public Check(final GrimPlayer player) {
        this.player = player;

        final Class<?> checkClass = this.getClass();

        if (checkClass.isAnnotationPresent(CheckData.class)) {
            final CheckData checkData = checkClass.getAnnotation(CheckData.class);
            this.checkName = checkData.name();
            this.configName = checkData.configName();
            this.flagCooldown = checkData.flagCooldown();
            this.buffer = checkData.buffer();
            this.maxBuffer = checkData.maxBuffer();
            this.vlMultiplier = checkData.decay();
            this.reset = checkData.reset();
            this.setback = checkData.setback();
        }

        reload();
    }

    public final void increaseViolations() {
        violations++;
        setbackIfAboveSetbackVL();
    }

    public final void reward() {
        violations -= decay;
    }

    public final double increaseBuffer() {
        return increaseBuffer(1);
    }

    public final double increaseBuffer(final double amount) {
        return buffer = Math.min(maxBuffer, buffer + amount);
    }

    public final double decreaseBuffer() {
        return decreaseBuffer(1);
    }

    public final double decreaseBuffer(final double amount) {
        return buffer = Math.max(0, buffer - amount);
    }

    public final void setBuffer(final double amount) {
        buffer = amount;
    }

    public final void multiplyBuffer(final double multiplier) {
        buffer *= multiplier;
    }

    public final void debug(final Object object) {
        player.user.sendMessage(ChatColor.AQUA + "[Debug] " + ChatColor.GREEN + object);
    }

    public final void broadcast(final Object object) {
        Bukkit.broadcastMessage(ChatColor.AQUA + "[GrimAC] " + ChatColor.GRAY + object);
    }

    public void reload() {
        decay = getConfig().getDouble(configName + ".decay");
        alertVL = getConfig().getDouble(configName + ".dont-alert-until");
        alertInterval = getConfig().getInt(configName + ".alert-interval");
        setbackVL = getConfig().getDouble(configName + ".setbackvl", Double.MAX_VALUE);

        secretTestServerVLStyle = getConfig().getBoolean("test-mode", false);

        if (alertVL == -1) alertVL = Double.MAX_VALUE;
        if (setbackVL == -1) alertVL = Double.MAX_VALUE;
    }

    public void alert(String verbose, String checkName, String violations) {
        // Not enough alerts to be sure that the player is cheating
        if (getViolations() < alertVL) return;
        // To reduce spam, some checks only alert 10% of the time
        if (alertInterval != 0 && alertCount++ % alertInterval != 0) return;

        String alertString = getConfig().getString("alerts.format", "%prefix% &f%player% &bfailed &f%check_name% &f(x&c%vl%&f) &7%verbose%");
        alertString = alertString.replace("%prefix%", getConfig().getString("prefix", "&bGrim &8»"));
        if (player.bukkitPlayer != null) {
            alertString = alertString.replace("%player%", player.bukkitPlayer.getName());
        }
        alertString = alertString.replace("%check_name%", checkName);
        alertString = alertString.replace("%vl%", violations);
        alertString = alertString.replace("%verbose%", verbose);

        if (!secretTestServerVLStyle) { // Production
            Bukkit.broadcast(ColorUtil.format(alertString), "grim.alerts");
        } else { // Test server
            player.user.sendMessage(ColorUtil.format(alertString));
        }

        GrimAPI.INSTANCE.getDiscordManager().sendAlert(player, checkName, violations, verbose);
    }

    public FileConfiguration getConfig() {
        return GrimAPI.INSTANCE.getPlugin().getConfig();
    }

    public void setbackIfAboveSetbackVL() {
        if (getViolations() > setbackVL) player.getSetbackTeleportUtil().executeSetback();
    }

    public String formatOffset(double offset) {
        return offset > 0.001 ? String.format("%.5f", offset) : String.format("%.2E", offset);
    }

    public String formatViolations() {
        return GrimMath.ceil(violations) + "";
    }
}

