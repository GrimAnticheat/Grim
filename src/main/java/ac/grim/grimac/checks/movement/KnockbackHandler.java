package ac.grim.grimac.checks.movement;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.TransactionKnockbackData;
import ac.grim.grimac.utils.data.VectorData;
import ac.grim.grimac.utils.data.VelocityData;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

// We are making a velocity sandwich between two pieces of transaction packets (bread)
public class KnockbackHandler {
    List<TransactionKnockbackData> firstBreadMap = new ArrayList<>();
    GrimPlayer player;

    List<VelocityData> lastKnockbackKnownTaken = new ArrayList<>();
    VelocityData firstBreadOnlyKnockback = null;

    public KnockbackHandler(GrimPlayer player) {
        this.player = player;
    }

    public void addPlayerKnockback(int entityID, int breadOne, Vector knockback) {
        double minimumMovement = 0.003D;
        if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.v_1_8))
            minimumMovement = 0.005D;

        if (Math.abs(knockback.getX()) < minimumMovement) {
            knockback.setX(0D);
        }

        if (Math.abs(knockback.getY()) < minimumMovement) {
            knockback.setY(0D);
        }

        if (Math.abs(knockback.getZ()) < minimumMovement) {
            knockback.setZ(0D);
        }

        firstBreadMap.add(new TransactionKnockbackData(breadOne, entityID, knockback));
    }

    public VelocityData getRequiredKB(int entityID, int transaction) {
        tickKnockback(transaction);

        VelocityData returnLastKB = null;
        for (VelocityData data : lastKnockbackKnownTaken) {
            if (data.entityID == entityID)
                returnLastKB = data;
        }

        lastKnockbackKnownTaken.clear();

        return returnLastKB;
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
            Bukkit.broadcastMessage(color + "Kb offset is " + offset);
        }
    }

    private void tickKnockback(int transactionID) {
        for (Iterator<TransactionKnockbackData> it = firstBreadMap.iterator(); it.hasNext(); ) {
            TransactionKnockbackData data = it.next();
            if (data.transactionID < transactionID) {
                lastKnockbackKnownTaken.add(new VelocityData(data.entityID, data.knockback));
                it.remove();
                firstBreadOnlyKnockback = null;
            } else if (data.transactionID - 1 == transactionID) { // First bread knockback
                firstBreadOnlyKnockback = new VelocityData(data.entityID, data.knockback);
            }
        }
    }

    public VelocityData getFirstBreadOnlyKnockback(int entityID, int transaction) {
        tickKnockback(transaction);
        if (firstBreadOnlyKnockback != null && firstBreadOnlyKnockback.entityID == entityID)
            return firstBreadOnlyKnockback;
        return null;
    }
}
