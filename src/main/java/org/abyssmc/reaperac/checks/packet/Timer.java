package org.abyssmc.reaperac.checks.packet;

import org.abyssmc.reaperac.GrimPlayer;
import org.abyssmc.reaperac.checks.movement.MovementCheck;
import org.bukkit.Bukkit;

public class Timer extends MovementCheck {
    private static final long millisPerTick = 50000000L;

    public Timer(GrimPlayer player) {
        long currentTime = System.nanoTime();

        player.offset += millisPerTick - (currentTime - player.lastMovementPacket);

        // Allow 0.5 seconds of "lagback"
        player.offset = Math.max(player.offset, -millisPerTick * 10);

        // 150 ms speed ahead = lagback
        // TODO: This causes a positive feedback loop with teleports!
        if (player.offset > (millisPerTick * 3)) {
            player.lagback();
        }

        Bukkit.broadcastMessage("Offset: " + (int) (player.offset / 1000000));

        player.lastMovementPacket = currentTime;
    }
}
