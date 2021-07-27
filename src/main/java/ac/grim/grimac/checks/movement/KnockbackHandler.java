package ac.grim.grimac.checks.movement;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.VectorData;
import ac.grim.grimac.utils.data.VelocityData;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import org.bukkit.ChatColor;
import org.bukkit.util.Vector;

// We are making a velocity sandwich between two pieces of transaction packets (bread)
public class KnockbackHandler {
    Short2ObjectOpenHashMap<Vector> firstBreadMap = new Short2ObjectOpenHashMap<>();
    GrimPlayer player;

    VelocityData lastKnockbackKnownTaken = null;
    VelocityData firstBreadOnlyKnockback = null;

    public KnockbackHandler(GrimPlayer player) {
        this.player = player;
    }

    public void handleTransactionPacket(short transactionID) {
        if (firstBreadMap.containsKey(transactionID)) {
            firstBreadOnlyKnockback = new VelocityData(firstBreadMap.get(transactionID));
        }

        if (firstBreadMap.containsKey((short) (transactionID + 1))) {
            firstBreadMap.remove((short) (transactionID + 1));

            lastKnockbackKnownTaken = firstBreadOnlyKnockback;
            firstBreadOnlyKnockback = null;
        }
    }

    public void addPlayerKnockback(short breadOne, Vector knockback) {
        firstBreadMap.put(breadOne, knockback);
    }

    public void handlePlayerKb(double offset) {

        if (player.possibleKB == null && player.firstBreadKB == null) {
            return;
        }

        if (!player.predictedVelocity.hasVectorType(VectorData.VectorType.Knockback))
            return;

        ChatColor color = ChatColor.GREEN;

        // Unsure knockback was taken
        if (player.firstBreadKB != null) {
            player.firstBreadKB.offset = Math.min(player.firstBreadKB.offset, offset);
        }

        // 100% known kb was taken
        if (player.possibleKB != null) {
            offset = Math.min(player.possibleKB.offset, offset);

            if (offset > 0.05) {
                color = ChatColor.RED;
            }

            // Add offset to violations
            //Bukkit.broadcastMessage(color + "Kb offset is " + offset);
        }
    }

    public VelocityData getRequiredKB() {
        VelocityData returnLastKB = lastKnockbackKnownTaken;
        lastKnockbackKnownTaken = null;

        return returnLastKB;
    }

    public VelocityData getFirstBreadOnlyKnockback() {
        return firstBreadOnlyKnockback;
    }
}
