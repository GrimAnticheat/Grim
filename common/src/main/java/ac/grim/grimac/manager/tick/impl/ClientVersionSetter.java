package ac.grim.grimac.manager.tick.impl;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.manager.tick.Tickable;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.LogUtil;
import com.github.retrooper.packetevents.netty.channel.ChannelHelper;

public class ClientVersionSetter implements Tickable {
    @Override
    public void tick() {
        long start = System.nanoTime();
        int count = 0;

        for (GrimPlayer player : GrimAPI.INSTANCE.getPlayerDataManager().getEntries()) {
            count++;

            // 1. SAFETY: Isolation. Don't let one player crash the loop.
            try {
                // Optional: Fast fail if channel closed (prevents some NPEs)
                if (player.user == null || !ChannelHelper.isOpen(player.user.getChannel())) {
                    continue;
                }

                player.pollData();
            } catch (Throwable t) {
                // 2. DEBUG: This will catch the "Uncaught Exception" if that theory is correct
                LogUtil.error("[CRITICAL] Error ticking specific player: " + player.getName(), t);
            }
        }

        long duration = System.nanoTime() - start;
        if (duration > 40_000_000) {
            LogUtil.warn("[DIAGNOSTIC] ClientVersionSetter took " + (duration / 1_000_000) + "ms for " + count + " players.");
        }
    }
}
