package ac.grim.grimac.checks.movement;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.player.GrimPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

public class TimerCheck extends Check {
    public static void processMovementPacket(GrimPlayer player) {
        // TODO: If the packet is the position reminder, increment by 20 instead of 1

        // lastTransactionReceived should use real time but as a proof of concept this is easier
        int lastTransactionReceived = player.lastTransactionReceived;
        int lastTransactionSent = player.lastTransactionSent.get();

        player.timerTransaction++;

        if (player.timerTransaction > lastTransactionSent + 1) {
            Bukkit.broadcastMessage(ChatColor.RED + player.bukkitPlayer.getName() + " is using timer!");

            // Reset violation for debugging purposes
            player.timerTransaction = Math.min(player.timerTransaction, player.lastLastTransactionReceived);
        }

        Bukkit.broadcastMessage("====================");
        Bukkit.broadcastMessage("Last last transaction " + player.lastLastTransactionReceived);
        Bukkit.broadcastMessage("Last transaction received " + player.lastTransactionReceived);
        Bukkit.broadcastMessage("Timer transaction " + player.timerTransaction);
        Bukkit.broadcastMessage("Last transaction sent " + player.lastTransactionSent);
        Bukkit.broadcastMessage("====================");

        player.timerTransaction = Math.max(player.timerTransaction, player.lastLastTransactionReceived);
    }
}
