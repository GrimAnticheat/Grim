package ac.grim.grimac.checks;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.ColorUtil;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

// Class from https://github.com/Tecnio/AntiCheatBase/blob/master/src/main/java/me/tecnio/anticheat/check/Check.java
@Getter
public class Check<T> {
    protected final GrimPlayer player;
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
            this.vlMultiplier = checkData.vlMultiplier();
            this.reset = checkData.reset();
            this.setback = checkData.setback();
        }

        reload();
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
        player.bukkitPlayer.sendMessage(ChatColor.AQUA + "[Debug] " + ChatColor.GREEN + object);
    }

    public final void broadcast(final Object object) {
        Bukkit.broadcastMessage(ChatColor.AQUA + "[GrimAC] " + ChatColor.GRAY + object);
    }

    public void reload() {

    }

    public void alert(String verbose, String checkName, String violations) {
        String alertString = getConfig().getString("alerts.format", "%prefix% &f%player% &bfailed &f%check_name% &f(x&c%vl%&f) %check-verbose%");
        alertString = alertString.replace("%prefix%", getConfig().getString("prefix", "&bGrimAC &f»"));
        alertString = alertString.replace("%player%", player.bukkitPlayer.getName());
        alertString = alertString.replace("%check_name%", checkName);
        alertString = alertString.replace("%vl%", violations);
        alertString = alertString.replace("%verbose%", verbose);

        Bukkit.broadcast(ColorUtil.format(alertString), "grim.alerts");
    }

    public FileConfiguration getConfig() {
        return GrimAPI.INSTANCE.getPlugin().getConfig();
    }

    public void setback() {
        player.getSetbackTeleportUtil().executeSetback(true);
    }
}

