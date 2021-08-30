package ac.grim.grimac.checks;

import ac.grim.grimac.player.GrimPlayer;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

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
            this.vlMultiplier = checkData.vlMultiplier();
            this.reset = checkData.reset();
            this.setback = checkData.setback();
        }
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
}

