package ac.grim.grimac.checks.impl.velocity;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import ac.grim.grimac.utils.data.VectorData;
import ac.grim.grimac.utils.data.VelocityData;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerExplosion;
import lombok.Getter;
import org.bukkit.util.Vector;

import java.util.Deque;
import java.util.LinkedList;

@CheckData(name = "AntiExplosion", configName = "Explosion", setback = 10)
public class ExplosionHandler extends Check implements PostPredictionCheck {
    private final Deque<VelocityData> firstBreadMap = new LinkedList<>();

    private VelocityData lastExplosionsKnownTaken = null;
    private VelocityData firstBreadAddedExplosion = null;

    @Getter
    private boolean explosionPointThree = false;

    private double offsetToFlag;
    private double setbackVL;

    public ExplosionHandler(final GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketSend(final PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.EXPLOSION) return;

        final WrapperPlayServerExplosion explosion = new WrapperPlayServerExplosion(event);

        final Vector3f velocity = explosion.getPlayerMotion();

        if (!explosion.getRecords().isEmpty()) {
            player.sendTransaction();

            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
                for (final Vector3i records : explosion.getRecords()) {
                    player.compensatedWorld.updateBlock(records.x, records.y, records.z, 0);
                }
            });
        }

        if (velocity.x != 0 || velocity.y != 0 || velocity.z != 0) {
            // No need to spam transactions
            if (explosion.getRecords().isEmpty()) player.sendTransaction();
            addPlayerExplosion(player.lastTransactionSent.get(), velocity);
            event.getTasksAfterSend().add(player::sendTransaction);
        }
    }

    public VelocityData getFutureExplosion() {
        // Chronologically in the future
        if (!firstBreadMap.isEmpty()) {
            return firstBreadMap.peek();
        }
        // Less in the future
        if (lastExplosionsKnownTaken != null) {
            return lastExplosionsKnownTaken;
        }
        // Uncertain, might be in the future
        if (player.firstBreadExplosion != null && player.likelyExplosions == null)
            return player.firstBreadExplosion;
        // Known to be in the present
        if (player.likelyExplosions != null)
            return player.likelyExplosions;
        return null;
    }

    public boolean shouldIgnoreForPrediction(final VectorData data) {
        if (data.isExplosion() && data.isFirstBreadExplosion()) {
            return player.firstBreadExplosion.offset > offsetToFlag;
        }
        return false;
    }

    public boolean wouldFlag() {
        return (player.likelyExplosions != null && player.likelyExplosions.offset > offsetToFlag) || (player.firstBreadExplosion != null && player.firstBreadExplosion.offset > offsetToFlag);
    }

    public void addPlayerExplosion(final int breadOne, final Vector3f explosion) {
        firstBreadMap.add(new VelocityData(-1, breadOne, player.getSetbackTeleportUtil().isSendingSetback, new Vector(explosion.getX(), explosion.getY(), explosion.getZ())));
    }

    public void setPointThree(final boolean isPointThree) {
        explosionPointThree = explosionPointThree || isPointThree;
    }

    public void handlePredictionAnalysis(final double offset) {
        if (player.firstBreadExplosion != null) {
            player.firstBreadExplosion.offset = Math.min(player.firstBreadExplosion.offset, offset);
        }

        if (player.likelyExplosions != null) {
            player.likelyExplosions.offset = Math.min(player.likelyExplosions.offset, offset);
        }
    }

    public void forceExempt() {
        // Unsure explosion was taken
        if (player.firstBreadExplosion != null) {
            player.firstBreadExplosion.offset = 0;
        }

        if (player.likelyExplosions != null) {
            player.likelyExplosions.offset = 0;
        }
    }

    @Override
    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        final double offset = predictionComplete.getOffset();

        final boolean wasZero = explosionPointThree;
        explosionPointThree = false;

        if (player.likelyExplosions == null && player.firstBreadExplosion == null) {
            firstBreadAddedExplosion = null;
            return;
        }

        // We must check to see if knockback has overridden this explosion
        // (Yes, I could make this very simple and exempt on kb, but that allows people to ignore most explosions)
        //
        // We do this by finding the minimum explosion transaction that could have been overridden
        // We then compare this against the maximum velocity transaction that could override
        //
        // If velocity is over transaction, exempt
        final int minTrans = Math.min(player.likelyExplosions != null ? player.likelyExplosions.transaction : Integer.MAX_VALUE,
                player.firstBreadExplosion != null ? player.firstBreadExplosion.transaction : Integer.MAX_VALUE);
        final int kbTrans = Math.max(player.likelyKB != null ? player.likelyKB.transaction : Integer.MIN_VALUE,
                player.firstBreadKB != null ? player.firstBreadKB.transaction : Integer.MIN_VALUE);

        if (player.predictedVelocity.isFirstBreadExplosion()) {
            firstBreadAddedExplosion = null;
            firstBreadMap.poll(); // Remove from map so we don't pull it again
        }

        if (wasZero || player.predictedVelocity.isExplosion() ||
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
        if (player.likelyExplosions == null) return;

        if (player.likelyExplosions.offset <= offsetToFlag) reward();

        if (flag()) {
            if (getViolations() > setbackVL) {
                player.getSetbackTeleportUtil().executeViolationSetback();
            }
        }

        final String formatOffset = player.likelyExplosions.offset == Integer.MAX_VALUE
                                    ? "ignored explosion"
                                    : "o: " + formatOffset(offset);

        alert(formatOffset);
    }


    public VelocityData getPossibleExplosions(final int lastTransaction, final boolean isJustTesting) {
        handleTransactionPacket(lastTransaction);
        if (lastExplosionsKnownTaken == null)
            return null;

        final VelocityData returnLastExplosion = lastExplosionsKnownTaken;
        if (!isJustTesting) {
            lastExplosionsKnownTaken = null;
        }
        return returnLastExplosion;
    }

    private void handleTransactionPacket(final int transactionID) {
        VelocityData data = firstBreadMap.peek();
        while (data != null) {
            if (data.transaction == transactionID) { // First bread explosion
                if (lastExplosionsKnownTaken != null)
                    firstBreadAddedExplosion = new VelocityData(-1, data.transaction, data.isSetback, lastExplosionsKnownTaken.vector.clone().add(data.vector));
                else
                    firstBreadAddedExplosion = new VelocityData(-1, data.transaction, data.isSetback, data.vector);
                break; // All knockback after this will have not been applied
            } else if (data.transaction < transactionID) {
                if (lastExplosionsKnownTaken != null) {
                    lastExplosionsKnownTaken.vector.add(data.vector);
                } else {
                    lastExplosionsKnownTaken = new VelocityData(-1, data.transaction, data.isSetback, data.vector);
                }

                firstBreadAddedExplosion = null;
                firstBreadMap.poll();
                data = firstBreadMap.peek();
            } else { // We are too far ahead in the future
                break;
            }
        }
    }

    public VelocityData getFirstBreadAddedExplosion(final int lastTransaction) {
        handleTransactionPacket(lastTransaction);
        return firstBreadAddedExplosion;
    }

    @Override
    public void reload() {
        super.reload();

        offsetToFlag = getConfig().getDoubleElse("Explosion.threshold", 0.00001);
        setbackVL = getConfig().getDoubleElse("Explosion.setbackvl", 10);

        if (setbackVL == -1) setbackVL = Double.MAX_VALUE;
    }
}
