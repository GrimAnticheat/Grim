package ac.grim.grimac.utils.latency;

import ac.grim.grimac.player.GrimPlayer;
import io.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.packetwrappers.play.out.explosion.WrappedPacketOutExplosion;
import io.github.retrooper.packetevents.packetwrappers.play.out.transaction.WrappedPacketOutTransaction;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class CompensatedExplosion {
    Long2ObjectMap<Vector> firstBreadMap = new Long2ObjectOpenHashMap<>();
    GrimPlayer player;

    Vector lastExplosionsKnownTaken = new Vector();
    Vector firstBreadAddedExplosion = null;

    boolean lastListHadFirstBreadKnockback = false;
    int breadValue = 0;

    public CompensatedExplosion(GrimPlayer player) {
        this.player = player;
    }

    public void handleTransactionPacket(int transactionID) {
        if (firstBreadMap.containsKey(transactionID)) {
            firstBreadAddedExplosion = lastExplosionsKnownTaken.clone().add(firstBreadMap.get(transactionID));
            breadValue = transactionID + 1;
        }

        if (firstBreadMap.containsKey(transactionID + 1)) {
            firstBreadAddedExplosion = null;
            lastExplosionsKnownTaken.add(firstBreadMap.remove(transactionID + 1));
        }
    }

    public void addPlayerExplosion(WrappedPacketOutExplosion explosion) {
        // Would this overflow if we got 32768?  no.
        // The limit returned by this would be 32767
        // We then keep this as an integer
        // Multiplying by 1 results in -32767
        // Subtracting 1 results in -32768, in the range of short
        int reservedID = (-1 * (player.lastTransactionSent.getAndAdd(2) % 32768));
        short breadOne = (short) reservedID;
        short breadTwo = (short) ((short) reservedID - 1);

        PacketEvents.get().getPlayerUtils().sendPacket(player.bukkitPlayer, new WrappedPacketOutTransaction(0, breadOne, false));
        PacketEvents.get().getPlayerUtils().sendPacket(player.bukkitPlayer, new WrappedPacketOutExplosion(explosion.getX(), explosion.getY(), explosion.getZ(), explosion.getStrength(), explosion.getRecords(), explosion.getPlayerMotionX(), explosion.getPlayerMotionY(), explosion.getPlayerMotionZ()));
        PacketEvents.get().getPlayerUtils().sendPacket(player.bukkitPlayer, new WrappedPacketOutTransaction(0, breadTwo, false));

        if (!firstBreadMap.containsKey(breadOne)) {
            firstBreadMap.put(breadOne, new Vector(explosion.getPlayerMotionX(), explosion.getPlayerMotionY(), explosion.getPlayerMotionZ()));
        }
    }

    public void setExplosionApplied(Vector knockback) {
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
    public void handlePlayerIgnoredExplosion() {
        if (player.possibleKB.size() != 1 || player.firstBreadKB == null) {
            Bukkit.broadcastMessage(ChatColor.RED + "Ignored kb " + player.possibleKB.get(0));
            Bukkit.broadcastMessage(ChatColor.RED + "PLAYER IS CHEATING! Knockback ignored");
        }
    }

    public List<Vector> getPossibleExplosions() {
        List<Vector> knockbackList = new ArrayList<>();
        lastListHadFirstBreadKnockback = false;

        if (firstBreadAddedExplosion != null) {
            knockbackList.add(firstBreadAddedExplosion);
            lastListHadFirstBreadKnockback = true;
        }

        if (lastExplosionsKnownTaken.getX() != 0 || lastExplosionsKnownTaken.getY() != 0 || lastExplosionsKnownTaken.getZ() != 0) {
            knockbackList.add(lastExplosionsKnownTaken);
            lastExplosionsKnownTaken = new Vector();
        }

        return knockbackList;
    }

    public Vector getFirstBreadAddedExplosion() {
        return firstBreadAddedExplosion;
    }
}
