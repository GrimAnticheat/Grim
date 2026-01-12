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
            // Check for closed channels here to see if ghosts exist in the map
            if (!com.github.retrooper.packetevents.netty.channel.ChannelHelper.isOpen(player.user.getChannel())) {
                 LogUtil.warn("[DIAGNOSTIC] Found GHOST in tick loop: " + player.getName());
            }

            player.pollData();
        }

        long duration = System.nanoTime() - start;
        if (duration > 40_000_000) { // Log if iteration takes > 40ms
            LogUtil.warn("[DIAGNOSTIC] ClientVersionSetter took " + (duration / 1_000_000) + "ms for " + count + " players.");
        }
    }
}
