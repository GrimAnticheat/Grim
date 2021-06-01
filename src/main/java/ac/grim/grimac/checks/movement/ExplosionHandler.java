package ac.grim.grimac.checks.movement;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.VectorData;
import ac.grim.grimac.utils.data.VelocityData;
import io.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.packetwrappers.play.out.explosion.WrappedPacketOutExplosion;
import io.github.retrooper.packetevents.packetwrappers.play.out.transaction.WrappedPacketOutTransaction;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.util.Vector;

public class ExplosionHandler {
    Short2ObjectOpenHashMap<Vector> firstBreadMap = new Short2ObjectOpenHashMap<>();
    GrimPlayer player;

    VelocityData lastExplosionsKnownTaken = new VelocityData(new Vector());
    VelocityData firstBreadAddedExplosion = null;

    public ExplosionHandler(GrimPlayer player) {
        this.player = player;
    }

    public void handleTransactionPacket(short transactionID) {
        if (firstBreadMap.containsKey(transactionID)) {
            firstBreadAddedExplosion = new VelocityData(lastExplosionsKnownTaken.vector.clone().add(firstBreadMap.get(transactionID)));
        }

        if (firstBreadMap.containsKey((short) (transactionID + 1))) {
            firstBreadAddedExplosion = null;
            lastExplosionsKnownTaken.vector.add(firstBreadMap.remove((short) (transactionID + 1)));
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
        short breadTwo = (short) (reservedID - 1);

        PacketEvents.get().getPlayerUtils().sendPacket(player.bukkitPlayer, new WrappedPacketOutTransaction(0, breadOne, false));
        PacketEvents.get().getPlayerUtils().sendPacket(player.bukkitPlayer, new WrappedPacketOutExplosion(explosion.getX(), explosion.getY(), explosion.getZ(), explosion.getStrength(), explosion.getRecords(), explosion.getPlayerMotionX(), explosion.getPlayerMotionY(), explosion.getPlayerMotionZ()));
        PacketEvents.get().getPlayerUtils().sendPacket(player.bukkitPlayer, new WrappedPacketOutTransaction(0, breadTwo, false));

        if (!firstBreadMap.containsKey(breadOne)) {
            firstBreadMap.put(breadOne, new Vector(explosion.getPlayerMotionX(), explosion.getPlayerMotionY(), explosion.getPlayerMotionZ()));
        }
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

    public VelocityData getPossibleExplosions() {
        if (lastExplosionsKnownTaken.vector.lengthSquared() < 1e-5)
            return null;

        VelocityData returnLastExplosion = lastExplosionsKnownTaken;
        lastExplosionsKnownTaken = new VelocityData(new Vector());

        return returnLastExplosion;
    }

    public VelocityData getFirstBreadAddedExplosion() {
        return firstBreadAddedExplosion;
    }
}
