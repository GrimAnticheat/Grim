package ac.grim.grimac.checks.movement;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.player.GrimPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

public class TimerCheck extends Check {
    public static void processMovementPacket(GrimPlayer grimPlayer) {
        // lastTransactionReceived should use real time but as a proof of concept this is easier
        int lastTransactionReceived = grimPlayer.lastTransactionReceived;
        int lastTransactionSent = grimPlayer.lastTransactionSent.get();

        grimPlayer.timerTransaction++;

        if (grimPlayer.timerTransaction > lastTransactionSent) {
            Bukkit.broadcastMessage(ChatColor.RED + grimPlayer.bukkitPlayer.getName() + " is using timer!");

            // Reset violation for debugging purposes
            grimPlayer.timerTransaction = Math.min(grimPlayer.timerTransaction, lastTransactionReceived);
        }

        grimPlayer.bukkitPlayer.sendMessage("==================");
        grimPlayer.bukkitPlayer.sendMessage("Sent: " + lastTransactionSent);
        grimPlayer.bukkitPlayer.sendMessage("Timer: " + grimPlayer.timerTransaction);
        grimPlayer.bukkitPlayer.sendMessage("Received: " + lastTransactionReceived);
        grimPlayer.bukkitPlayer.sendMessage("==================");
        grimPlayer.timerTransaction = Math.max(grimPlayer.timerTransaction, lastTransactionReceived);
    }
}
