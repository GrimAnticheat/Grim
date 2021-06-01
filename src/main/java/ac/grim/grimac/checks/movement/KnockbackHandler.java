package ac.grim.grimac.checks.movement;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.VectorData;
import ac.grim.grimac.utils.data.VelocityData;
import io.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.packetwrappers.play.out.entityvelocity.WrappedPacketOutEntityVelocity;
import io.github.retrooper.packetevents.packetwrappers.play.out.transaction.WrappedPacketOutTransaction;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.util.Vector;

// We are making a velocity sandwich between two pieces of transaction packets (bread)
public class KnockbackHandler {
    Int2ObjectLinkedOpenHashMap<Vector> firstBreadMap = new Int2ObjectLinkedOpenHashMap<>();
    GrimPlayer player;

    VelocityData lastKnockbackKnownTaken = null;
    VelocityData firstBreadOnlyKnockback = null;

    public KnockbackHandler(GrimPlayer player) {
        this.player = player;
    }

    public void handleTransactionPacket(int transactionID) {
        if (firstBreadMap.containsKey(transactionID)) {
            firstBreadOnlyKnockback = new VelocityData(firstBreadMap.get(transactionID));
        }

        if (firstBreadMap.containsKey(transactionID + 1)) {
            firstBreadMap.remove(transactionID + 1);

            lastKnockbackKnownTaken = firstBreadOnlyKnockback;
            firstBreadOnlyKnockback = null;
        }
    }

    public void addPlayerKnockback(Vector knockback) {
        // Would this overflow if we got 32768?  no.
        // The limit returned by this would be 32767
        // We then keep this as an integer
        // Multiplying by 1 results in -32767
        // Subtracting 1 results in -32768, in the range of short
        int reservedID = (-1 * (player.lastTransactionSent.getAndAdd(2) % 32768));
        short breadOne = (short) reservedID;
        short breadTwo = (short) (reservedID - 1);

        PacketEvents.get().getPlayerUtils().sendPacket(player.bukkitPlayer, new WrappedPacketOutTransaction(0, breadOne, false));
        PacketEvents.get().getPlayerUtils().sendPacket(player.bukkitPlayer, new WrappedPacketOutEntityVelocity(player.entityID, knockback.getX(), knockback.getY(), knockback.getZ()));
        PacketEvents.get().getPlayerUtils().sendPacket(player.bukkitPlayer, new WrappedPacketOutTransaction(0, breadTwo, false));

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
            Bukkit.broadcastMessage(color + "Kb offset is " + offset);
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
