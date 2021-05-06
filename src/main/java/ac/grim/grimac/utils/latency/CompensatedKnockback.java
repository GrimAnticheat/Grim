package ac.grim.grimac.utils.latency;

import ac.grim.grimac.player.GrimPlayer;
import io.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.packetwrappers.play.out.entityvelocity.WrappedPacketOutEntityVelocity;
import io.github.retrooper.packetevents.packetwrappers.play.out.transaction.WrappedPacketOutTransaction;
import io.github.retrooper.packetevents.utils.list.ConcurrentList;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.bukkit.Bukkit;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CompensatedKnockback {
    ConcurrentHashMap<Integer, ConcurrentList<Vector>> requiredKnockback = new ConcurrentHashMap<>();
    Long2ObjectMap<Vector> firstBreadMap = new Long2ObjectOpenHashMap<>();
    GrimPlayer player;

    ConcurrentHashMap<Integer, GrimPlayer> transactionMap = new ConcurrentHashMap<>();

    public CompensatedKnockback(GrimPlayer player) {
        this.player = player;
    }

    public void handleTransactionPacket(int transactionID) {
        if (firstBreadMap.containsKey(transactionID)) {
            Bukkit.broadcastMessage("Sandwich began!");
        }

        if (firstBreadMap.containsKey(transactionID + 1)) {
            Bukkit.broadcastMessage("Sandwich complete!");
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
        // TODO:
    }

    public List<Vector> getPossibleKnockback(int lastTransactionReceived) {
        List<Vector> knockbackList = new ArrayList<>();

        Iterator<Map.Entry<Integer, ConcurrentList<Vector>>> iterator = requiredKnockback.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, ConcurrentList<Vector>> knockback = iterator.next();

            // 20 is minimum ticks per movement packet
            if (knockback.getKey() - 20 > lastTransactionReceived) continue;

            if (knockback.getKey() < player.lastTransactionReceived) {
                Bukkit.broadcastMessage("Player ignored kb!");
                iterator.remove();
                continue;
            }

            knockbackList.addAll(knockback.getValue());
        }

        return knockbackList;
    }
}
