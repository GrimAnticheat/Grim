package ac.grim.grimac.utils.latency;

import ac.grim.grimac.player.GrimPlayer;
import io.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.packetwrappers.play.out.entityvelocity.WrappedPacketOutEntityVelocity;
import io.github.retrooper.packetevents.packetwrappers.play.out.transaction.WrappedPacketOutTransaction;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class CompensatedKnockback {
    Long2ObjectMap<Vector> firstBreadMap = new Long2ObjectOpenHashMap<>();
    GrimPlayer player;

    List<Vector> possibleKnockbackValuesTaken = new ArrayList<>();
    Vector firstBreadOnlyKnockback = null;

    public CompensatedKnockback(GrimPlayer player) {
        this.player = player;
    }

    public void handleTransactionPacket(int transactionID) {
        if (firstBreadMap.containsKey(transactionID)) {
            firstBreadOnlyKnockback = firstBreadMap.get(transactionID);
        }

        if (firstBreadMap.containsKey(transactionID + 1)) {
            firstBreadOnlyKnockback = null;
            possibleKnockbackValuesTaken.add(firstBreadMap.get(transactionID + 1));
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
        short breadTwo = (short) ((short) reservedID - 1);

        PacketEvents.get().getPlayerUtils().sendPacket(player.bukkitPlayer, new WrappedPacketOutTransaction(0, breadOne, false));
        PacketEvents.get().getPlayerUtils().sendPacket(player.bukkitPlayer, new WrappedPacketOutEntityVelocity(player.entityID, knockback.getX(), knockback.getY(), knockback.getZ()));
        PacketEvents.get().getPlayerUtils().sendPacket(player.bukkitPlayer, new WrappedPacketOutTransaction(0, breadTwo, false));

        if (!firstBreadMap.containsKey(breadOne)) {
            firstBreadMap.put(breadOne, knockback);
        }
    }

    // TODO: Handle setting firstBreadOnlyKnockback to null if it is used
    public void setPlayerKnockbackApplied(Vector knockback) {

    }

    // This will be called if there is kb taken but it isn't applied to the player
    public void setKnockbackDenied(Vector knockback) {

    }

    public List<Vector> getPossibleKnockback() {
        if (firstBreadOnlyKnockback != null) {
            List<Vector> knockbackList = new ArrayList<>(possibleKnockbackValuesTaken);
            knockbackList.add(firstBreadOnlyKnockback);
            return knockbackList;
        }

        List<Vector> lastKBList = possibleKnockbackValuesTaken;
        possibleKnockbackValuesTaken = new ArrayList<>();

        return lastKBList;
    }

    public Vector getFirstBreadOnlyKnockback() {
        return firstBreadOnlyKnockback;
    }
}
