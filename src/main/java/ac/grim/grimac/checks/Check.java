package ac.grim.grimac.checks;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.events.FlagEvent;
import ac.grim.grimac.utils.math.GrimMath;
import github.scarsz.configuralize.DynamicConfig;
import lombok.Getter;
import org.bukkit.Bukkit;

// Class from https://github.com/Tecnio/AntiCheatBase/blob/master/src/main/java/me/tecnio/anticheat/check/Check.java
@Getter
public class Check<T> {
    protected final GrimPlayer player;

    public double violations;
    public double decay;
    public double setbackVL;

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
        }

        reload();
    }

    public void flagAndAlert() {
        if (flag()) {
            alert("", formatViolations());
        }
    }

    public final boolean flag() {
        FlagEvent event = new FlagEvent(this);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;

        player.punishmentManager.handleViolation(this);

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

    public void reload() {
        decay = getConfig().getDoubleElse(configName + ".decay", decay);
        setbackVL = getConfig().getDoubleElse(configName + ".setbackvl", setbackVL);

        if (setbackVL == -1) setbackVL = Double.MAX_VALUE;
    }

    public void alert(String verbose, String violations) {
        player.punishmentManager.handleAlert(player, verbose, this, violations);
    }

    public DynamicConfig getConfig() {
        return GrimAPI.INSTANCE.getConfigManager().getConfig();
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

