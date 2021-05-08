package ac.grim.grimac.checks.movement;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.player.GrimPlayer;

public class TimerCheck extends Check {
    public static void processMovementPacket(GrimPlayer player) {
        // TODO: If the packet is the position reminder, increment by 20 instead of 1

        // lastTransactionReceived should use real time but as a proof of concept this is easier
        int lastTransactionReceived = player.lastTransactionReceived;
        int lastTransactionSent = player.lastTransactionSent.get();

        player.timerTransaction++;

        if (player.timerTransaction > lastTransactionSent + 1) {
            //Bukkit.broadcastMessage(ChatColor.RED + player.bukkitPlayer.getName() + " is using timer!");

            // Reset violation for debugging purposes
            player.timerTransaction = Math.min(player.timerTransaction, player.lastLastTransactionReceived);
        }

        player.timerTransaction = Math.max(player.timerTransaction, player.lastLastTransactionReceived);
    }
}
