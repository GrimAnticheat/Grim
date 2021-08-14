package ac.grim.grimac.checks.movement;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.TransactionKnockbackData;
import ac.grim.grimac.utils.data.VectorData;
import ac.grim.grimac.utils.data.VelocityData;
import io.github.retrooper.packetevents.utils.vector.Vector3f;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.util.Vector;

import java.util.concurrent.ConcurrentLinkedQueue;

public class ExplosionHandler {
    ConcurrentLinkedQueue<TransactionKnockbackData> firstBreadMap = new ConcurrentLinkedQueue<>();
    GrimPlayer player;

    Vector lastExplosionsKnownTaken = null;
    Vector firstBreadAddedExplosion = null;

    public ExplosionHandler(GrimPlayer player) {
        this.player = player;
    }

    public void addPlayerExplosion(int breadOne, Vector3f explosion) {
        firstBreadMap.add(new TransactionKnockbackData(breadOne, null, new Vector(explosion.getX(), explosion.getY(), explosion.getZ())));
    }

    public void handlePlayerExplosion(double offset, boolean force) {
        if (player.likelyExplosions == null && player.firstBreadExplosion == null) {
            return;
        }

        if (force || player.predictedVelocity.hasVectorType(VectorData.VectorType.Explosion)) {
            // Unsure knockback was taken
            if (player.firstBreadExplosion != null) {
                player.firstBreadExplosion.offset = Math.min(player.firstBreadExplosion.offset, offset);
            }

            if (player.likelyExplosions != null) {
                player.likelyExplosions.offset = Math.min(player.likelyExplosions.offset, offset);
            }
        }

        // 100% known kb was taken
        if (player.likelyExplosions != null) {
            ChatColor color = ChatColor.GREEN;
            if (player.likelyExplosions.offset > 0.05) {
                color = ChatColor.RED;
            }
            // Add offset to violations
            Bukkit.broadcastMessage(color + "Explosion offset is " + player.likelyExplosions.offset);
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
        TransactionKnockbackData data = firstBreadMap.peek();
        while (data != null) {
            if (data.transactionID == transactionID) { // First bread explosion
                if (lastExplosionsKnownTaken != null)
                    firstBreadAddedExplosion = lastExplosionsKnownTaken.clone().add(data.knockback);
                else
                    firstBreadAddedExplosion = data.knockback;
                break; // All knockback after this will have not been applied
            } else if (data.transactionID < transactionID) {
                if (lastExplosionsKnownTaken != null)
                    lastExplosionsKnownTaken.add(data.knockback);
                else
                    lastExplosionsKnownTaken = data.knockback;

                firstBreadAddedExplosion = null;
                firstBreadMap.poll();
                data = firstBreadMap.peek();
            } else { // We are too far ahead in the future
                break;
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
