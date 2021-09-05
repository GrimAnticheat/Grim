package ac.grim.grimac.checks.impl.velocity;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.VectorData;
import ac.grim.grimac.utils.data.VelocityData;
import ac.grim.grimac.utils.math.GrimMath;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.play.out.explosion.WrappedPacketOutExplosion;
import io.github.retrooper.packetevents.utils.vector.Vector3f;
import org.bukkit.util.Vector;

import java.util.concurrent.ConcurrentLinkedQueue;

@CheckData(name = "AntiExplosion", configName = "Explosion")
public class ExplosionHandler extends PacketCheck {
    ConcurrentLinkedQueue<VelocityData> firstBreadMap = new ConcurrentLinkedQueue<>();
    GrimPlayer player;

    VelocityData lastExplosionsKnownTaken = null;
    VelocityData firstBreadAddedExplosion = null;

    boolean wasKbZeroPointZeroThree = false;

    double offsetToFlag;
    double setbackVL;

    public ExplosionHandler(GrimPlayer player) {
        super(player);
        this.player = player;
    }

    @Override
    public void onPacketSend(final PacketPlaySendEvent event) {
        if (event.getPacketId() == PacketType.Play.Server.EXPLOSION) {
            WrappedPacketOutExplosion explosion = new WrappedPacketOutExplosion(event.getNMSPacket());

            Vector3f velocity = explosion.getPlayerVelocity();

            if (velocity.x != 0 || velocity.y != 0 || velocity.z != 0) {
                GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
                if (player == null) return;

                player.sendTransactionOrPingPong(player.getNextTransactionID(1), false);
                addPlayerExplosion(player.lastTransactionSent.get(), velocity);
                event.setPostTask(player::sendAndFlushTransactionOrPingPong);
            }
        }
    }

    public void addPlayerExplosion(int breadOne, Vector3f explosion) {
        firstBreadMap.add(new VelocityData(-1, breadOne, new Vector(explosion.getX(), explosion.getY(), explosion.getZ())));
    }

    public void handlePredictionAnalysis(double offset, Vector vector) {
        if (vector.lengthSquared() < player.uncertaintyHandler.getZeroPointZeroThreeThreshold())
            wasKbZeroPointZeroThree = true;

        if (player.firstBreadExplosion != null) {
            player.firstBreadExplosion.offset = Math.min(player.firstBreadExplosion.offset, offset);
        }

        if (player.likelyExplosions != null) {
            player.likelyExplosions.offset = Math.min(player.likelyExplosions.offset, offset);
        }
    }

    public void handlePlayerExplosion(double offset, boolean force) {
        boolean wasZero = wasKbZeroPointZeroThree;
        wasKbZeroPointZeroThree = false;

        if (player.likelyExplosions == null && player.firstBreadExplosion == null) {
            return;
        }

        // We must check to see if knockback has overridden this explosion
        // (Yes, I could make this very simple and exempt on kb, but that allows people to ignore most explosions)
        //
        // We do this by finding the minimum explosion transaction that could have been overridden
        // We then compare this against the maximum velocity transaction that could override
        //
        // If velocity is over transaction, exempt
        int minTrans = Math.min(player.likelyExplosions != null ? player.likelyExplosions.transaction : Integer.MAX_VALUE,
                player.firstBreadExplosion != null ? player.firstBreadExplosion.transaction : Integer.MAX_VALUE);
        int kbTrans = Math.max(player.likelyKB != null ? player.likelyKB.transaction : Integer.MIN_VALUE,
                player.firstBreadKB != null ? player.firstBreadKB.transaction : Integer.MIN_VALUE);

        if (!force && !wasZero && player.predictedVelocity.hasVectorType(VectorData.VectorType.Knockback) &&
                player.likelyExplosions == null && player.firstBreadExplosion != null) {
            // The player took this knockback, this tick, 100%
            // Fixes exploit that would allow players to take explosions an infinite number of times
            if (player.firstBreadExplosion.offset < offsetToFlag) {
                firstBreadAddedExplosion = null;
            }
        }

        if (force || wasZero || player.predictedVelocity.hasVectorType(VectorData.VectorType.Explosion) ||
                (minTrans < kbTrans)) {
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
            if (player.likelyExplosions.offset > offsetToFlag) {
                increaseViolations();

                String formatOffset = formatOffset(offset);
                alert("o: " + formatOffset, "AntiExplosion", GrimMath.floor(violations) + "");
            } else {
                reward();
            }
        }
    }

    public VelocityData getPossibleExplosions(int lastTransaction) {
        handleTransactionPacket(lastTransaction);
        if (lastExplosionsKnownTaken == null)
            return null;

        VelocityData returnLastExplosion = lastExplosionsKnownTaken;
        lastExplosionsKnownTaken = null;

        return returnLastExplosion;
    }

    private void handleTransactionPacket(int transactionID) {
        VelocityData data = firstBreadMap.peek();
        while (data != null) {
            if (data.transaction == transactionID) { // First bread explosion
                if (lastExplosionsKnownTaken != null)
                    firstBreadAddedExplosion = new VelocityData(-1, data.transaction, lastExplosionsKnownTaken.vector.clone().add(data.vector));
                else
                    firstBreadAddedExplosion = new VelocityData(-1, data.transaction, data.vector);
                break; // All knockback after this will have not been applied
            } else if (data.transaction < transactionID) {
                if (lastExplosionsKnownTaken != null)
                    lastExplosionsKnownTaken.vector.clone().add(data.vector);
                else {
                    if (firstBreadAddedExplosion != null) // Bring over the previous offset, don't require explosions twice
                        lastExplosionsKnownTaken = new VelocityData(-1, data.transaction, data.vector, firstBreadAddedExplosion.offset);
                    else
                        lastExplosionsKnownTaken = new VelocityData(-1, data.transaction, data.vector);
                }

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
        return firstBreadAddedExplosion;
    }

    @Override
    public void reload() {
        super.reload();

        offsetToFlag = getConfig().getDouble("Knockback.threshold", 0.00001);
        setbackVL = getConfig().getDouble("Knockback.setbackvl", 10);
    }
}
