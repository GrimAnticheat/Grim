package ac.grim.grimac.checks.movement;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.player.GrimPlayer;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

public class TimerCheck extends Check {
    public long lastTransactionPing = Integer.MIN_VALUE;
    public long transactionPing = Integer.MIN_VALUE;
    GrimPlayer player;
    double packetX = Double.MAX_VALUE;
    double packetY = Double.MAX_VALUE;
    double packetZ = Double.MAX_VALUE;
    float packetXRot = Float.MAX_VALUE;
    float packetYRot = Float.MAX_VALUE;
    long timerTransaction = Integer.MIN_VALUE;
    boolean isFirst = true;

    public TimerCheck(GrimPlayer player) {
        this.player = player;
    }

    public void processMovementPacket(double playerX, double playerY, double playerZ, float xRot, float yRot) {
        boolean isReminder = playerX == packetX && playerY == packetY && playerZ == packetZ && packetXRot == xRot && packetYRot == yRot && !isFirst;

        // 1.8 clients spam movement packets every tick, even if they didn't move
        if (isReminder && player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_9)) timerTransaction += 950;

        timerTransaction += 50;
        player.movementPackets++;

        if (timerTransaction > System.currentTimeMillis()) {
            Bukkit.broadcastMessage(ChatColor.RED + player.bukkitPlayer.getName() + " is using timer!");

            // Reset violation for debugging purposes
            timerTransaction = Math.min(timerTransaction, lastTransactionPing);
        }

        timerTransaction = Math.max(timerTransaction, lastTransactionPing);

        this.packetX = playerX;
        this.packetY = playerY;
        this.packetZ = playerZ;
        this.packetXRot = xRot;
        this.packetYRot = yRot;

        this.lastTransactionPing = transactionPing;
        this.transactionPing = System.currentTimeMillis() - player.getTransactionPing();

        isFirst = false;
    }
}
