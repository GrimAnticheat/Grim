package ac.grim.grimac.manager.init.start;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.manager.init.Initable;
import ac.grim.grimac.player.GrimPlayer;
import org.bukkit.Bukkit;

public class PacketLimiter implements Initable {
    @Override
    public void start() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(GrimAPI.INSTANCE.getPlugin(), () -> {
            for (GrimPlayer player : GrimAPI.INSTANCE.getPlayerDataManager().getEntries()) {
                // Avoid concurrent reading on an integer as it's results are unknown
                player.cancelledPackets.set(0);
            }
        }, 0, 20);
    }
}
