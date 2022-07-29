package ac.grim.grimac.manager.init.start;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.manager.init.Initable;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.LogUtil;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PacketLimiter implements Initable {
    @Override
    public void start() {
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
        executorService.scheduleAtFixedRate(() -> {
            for (GrimPlayer player : GrimAPI.INSTANCE.getPlayerDataManager().getEntries()) {
                // Avoid concurrent reading on an integer as it's results are unknown
                if (player.cancelledPackets.get() > GrimAPI.INSTANCE.getConfigManager().getConfig().getIntElse("packet-spam-threshold", 100)) {
                    LogUtil.info("Disconnecting " + player.user.getName() + " for spamming invalid packets, packets cancelled in a second " + player.cancelledPackets);
                    player.user.closeConnection();
                }
                player.cancelledPackets.set(0);
            }
        }, 1, 1, TimeUnit.SECONDS);
    }
}
