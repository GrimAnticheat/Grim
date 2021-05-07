package ac.grim.grimac.utils.latency;

import ac.grim.grimac.player.GrimPlayer;
import io.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.packetwrappers.play.out.entityvelocity.WrappedPacketOutEntityVelocity;
import io.github.retrooper.packetevents.packetwrappers.play.out.transaction.WrappedPacketOutTransaction;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

// We are making a velocity sandwich between two pieces of bread
public class CompensatedKnockback {
    Long2ObjectMap<Vector> firstBreadMap = new Long2ObjectOpenHashMap<>();
    GrimPlayer player;

    Vector lastKnockbackKnownTaken = null;
    Vector firstBreadOnlyKnockback = null;

    boolean lastListHadFirstBreadKnockback = false;
    int breadValue = 0;

    public CompensatedKnockback(GrimPlayer player) {
        this.player = player;
    }

    public void handleTransactionPacket(int transactionID) {
        if (firstBreadMap.containsKey(transactionID)) {
            firstBreadOnlyKnockback = firstBreadMap.get(transactionID);
            breadValue = transactionID + 1;
        }

        if (firstBreadMap.containsKey(transactionID + 1)) {
            firstBreadOnlyKnockback = null;
            lastKnockbackKnownTaken = firstBreadMap.get(transactionID + 1);
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

    public void setPlayerKnockbackApplied(Vector knockback) {
        // How to be a legit client and flag this check:
        // First you must take multiple knockback values combined to arrive before the same movement packet
        // This is unlikely
        // Next, the last velocity must have the first bread arrive and the velocity not arrive
        // This is unlikely
        //
        // As velocity checks will be much more strict than regular movement checks, this flags movement and not velocity
        //
        // There is a fix for this, but it would allow cheaters to take knockback twice 100% of the time, which is worse IMO
        // One of the few cases where false positives are better than lenience
        //
        // So just set it to null and be sad :(
        //
        // Hack to remove first bread data from an unknown number of next predictions
        Vector markRemoved = player.firstBreadKB;

        if (knockback.equals(markRemoved)) {
            markRemoved.setX(129326);
            markRemoved.setY(741979);
            markRemoved.setZ(916042);
        }
    }

    // This will be called if there is kb taken but it isn't applied to the player
    public void handlePlayerIgnoredKB() {
        if (player.possibleKB.size() != 1 || player.firstBreadKB == null) {
            Bukkit.broadcastMessage(ChatColor.RED + "Ignored kb " + player.possibleKB.get(0));
            Bukkit.broadcastMessage(ChatColor.RED + "PLAYER IS CHEATING! Knockback ignored");
        }
    }

    public List<Vector> getPossibleKnockback() {
        List<Vector> knockbackList = new ArrayList<>();
        lastListHadFirstBreadKnockback = false;

        if (firstBreadOnlyKnockback != null) {
            knockbackList.add(firstBreadOnlyKnockback);
            lastListHadFirstBreadKnockback = true;
        }

        if (lastKnockbackKnownTaken != null) {
            knockbackList.add(lastKnockbackKnownTaken);
            lastKnockbackKnownTaken = null;
        }

        return knockbackList;
    }

    public Vector getFirstBreadOnlyKnockback() {
        return firstBreadOnlyKnockback;
    }
}
