package ac.grim.grimac.checks;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.commands.GrimAlerts;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import ac.grim.grimac.utils.events.FlagEvent;
import ac.grim.grimac.utils.math.GrimMath;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

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

    private String checkName;
    private String configName;

    public Check(final GrimPlayer player) {
        this.player = player;

        final Class<?> checkClass = this.getClass();

        if (checkClass.isAnnotationPresent(CheckData.class)) {
            final CheckData checkData = checkClass.getAnnotation(CheckData.class);
            this.checkName = checkData.name();
            this.configName = checkData.configName();
            // Fall back to check name
            if (this.configName.equals("DEFAULT")) this.configName = this.checkName;
            this.decay = checkData.decay();
            this.setbackVL = checkData.setback();
            this.alertVL = checkData.dontAlertUntil();
            this.alertInterval = checkData.alertInterval();
        }

        reload();
    }

    public void flagAndAlert() {
        if (flag()) {
            alert("", getCheckName(), formatViolations());
        }
    }

    public final boolean flag() {
        FlagEvent event = new FlagEvent(this);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;

        violations++;
        return true;
    }

    public final void flagWithSetback() {
        if (flag()) {
            setbackIfAboveSetbackVL();
        }
    }

    public final void reward() {
        violations -= decay;
    }

    public final void debug(final Object object) {
        player.user.sendMessage(ChatColor.AQUA + "[Debug] " + ChatColor.GREEN + object);
    }

    public final void broadcast(final Object object) {
        Bukkit.broadcastMessage(ChatColor.AQUA + "[GrimAC] " + ChatColor.GRAY + object);
    }

    public void reload() {
        decay = getConfig().getDouble(configName + ".decay", decay);
        alertVL = getConfig().getDouble(configName + ".dont-alert-until", alertVL);
        alertInterval = getConfig().getInt(configName + ".alert-interval", alertInterval);
        setbackVL = getConfig().getDouble(configName + ".setbackvl", setbackVL);

        if (alertVL == -1) alertVL = Double.MAX_VALUE;
        if (setbackVL == -1) setbackVL = Double.MAX_VALUE;
    }

    public boolean shouldAlert() {
        // Not enough alerts to be sure that the player is cheating
        if (getViolations() < alertVL) return false;
        // To reduce spam, some checks only alert 10% of the time
        return alertInterval == 0 || alertCount % alertInterval == 0;
    }

    public void alert(String verbose, String checkName, String violations) {
        alertCount++;
        if (!shouldAlert()) return;

        GrimAPI.INSTANCE.getAlertManager().sendAlert(player, verbose, checkName, violations);
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

