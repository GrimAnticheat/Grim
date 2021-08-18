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

    private String checkName;
    private int threshold;
    private long reset;

    public Check(final GrimPlayer player) {
        this.player = player;

        final Class<?> checkClass = this.getClass();

        if (checkClass.isAnnotationPresent(CheckData.class)) {
            final CheckData checkData = checkClass.getAnnotation(CheckData.class);
            this.checkName = checkData.name();
            this.threshold = checkData.threshold();
            this.reset = checkData.reset();
        }
    }

    public final double increaseBuffer() {
        return increaseBuffer(1);
    }

    public final double increaseBuffer(final double amount) {
        return buffer = Math.min(10000, buffer + amount);
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
        player.bukkitPlayer.sendMessage(ChatColor.AQUA + "[GrimDebug] " + ChatColor.GREEN + object);
    }

    public final void broadcast(final Object object) {
        Bukkit.broadcastMessage(ChatColor.AQUA + "[GrimBroadcast] " + ChatColor.GRAY + object);
    }
}

