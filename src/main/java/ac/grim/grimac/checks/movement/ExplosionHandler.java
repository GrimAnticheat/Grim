package ac.grim.grimac.checks.movement;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.TransactionKnockbackData;
import ac.grim.grimac.utils.data.VectorData;
import ac.grim.grimac.utils.data.VelocityData;
import io.github.retrooper.packetevents.utils.vector.Vector3f;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ExplosionHandler {
    List<TransactionKnockbackData> firstBreadMap = new ArrayList<>();
    GrimPlayer player;

    Vector lastExplosionsKnownTaken = null;
    Vector firstBreadAddedExplosion = null;

    public ExplosionHandler(GrimPlayer player) {
        this.player = player;
    }

    public void addPlayerExplosion(int breadOne, Vector3f explosion) {
        firstBreadMap.add(new TransactionKnockbackData(breadOne, null, new Vector(explosion.getX(), explosion.getY(), explosion.getZ())));
    }

    public void handlePlayerExplosion(double offset) {
        if (player.knownExplosion == null && player.firstBreadExplosion == null) {
            return;
        }

        ChatColor color = ChatColor.GREEN;

        if (!player.predictedVelocity.hasVectorType(VectorData.VectorType.Explosion))
            return;

        // Unsure knockback was taken
        if (player.firstBreadExplosion != null) {
            player.firstBreadExplosion.offset = Math.min(player.firstBreadExplosion.offset, offset);
        }

        // 100% known kb was taken
        if (player.knownExplosion != null) {
            offset = Math.min(player.knownExplosion.offset, offset);

            if (offset > 0.05) {
                color = ChatColor.RED;
            }

            // Add offset to violations
            Bukkit.broadcastMessage(color + "Explosion offset is " + offset);
        }
    }

    public VelocityData getPossibleExplosions(int lastTransaction) {
        handleTransactionPacket(lastTransaction);
        if (lastExplosionsKnownTaken == null)
            return null;

        VelocityData returnLastExplosion = new VelocityData(-1, lastExplosionsKnownTaken);
        lastExplosionsKnownTaken = null;

        return returnLastExplosion;
    }

    private void handleTransactionPacket(int transactionID) {
        for (Iterator<TransactionKnockbackData> it = firstBreadMap.iterator(); it.hasNext(); ) {
            TransactionKnockbackData data = it.next();
            if (data.transactionID < transactionID) {
                if (lastExplosionsKnownTaken != null)
                    lastExplosionsKnownTaken.add(data.knockback);
                else
                    lastExplosionsKnownTaken = data.knockback;
                it.remove();

                firstBreadAddedExplosion = null;
            } else if (data.transactionID - 1 == transactionID) { // First bread explosion
                if (lastExplosionsKnownTaken != null)
                    firstBreadAddedExplosion = lastExplosionsKnownTaken.clone().add(data.knockback);
                else
                    firstBreadAddedExplosion = data.knockback;
            }
        }
    }

    public VelocityData getFirstBreadAddedExplosion(int lastTransaction) {
        handleTransactionPacket(lastTransaction);
        if (firstBreadAddedExplosion == null)
            return null;
        return new VelocityData(-1, firstBreadAddedExplosion);
    }
}
