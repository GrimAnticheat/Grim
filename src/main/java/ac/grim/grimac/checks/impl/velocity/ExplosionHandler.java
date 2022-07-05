package ac.grim.grimac.checks.impl.velocity;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.VelocityData;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerExplosion;
import org.bukkit.util.Vector;

import java.util.concurrent.ConcurrentLinkedQueue;

@CheckData(name = "AntiExplosion", configName = "Explosion", setback = 10)
public class ExplosionHandler extends PacketCheck {
    ConcurrentLinkedQueue<VelocityData> firstBreadMap = new ConcurrentLinkedQueue<>();

    VelocityData lastExplosionsKnownTaken = null;
    VelocityData firstBreadAddedExplosion = null;

    boolean wasKbZeroPointZeroThree = false;

    double offsetToFlag;
    double setbackVL;

    public ExplosionHandler(GrimPlayer player) {
        super(player);
    }

    public boolean isPendingExplosion() {
        return firstBreadMap.size() > 0;
    }

    @Override
    public void onPacketSend(final PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.EXPLOSION) {
            WrapperPlayServerExplosion explosion = new WrapperPlayServerExplosion(event);

            Vector3f velocity = explosion.getPlayerMotion();

            if (!explosion.getRecords().isEmpty()) {
                player.sendTransaction();

                player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
                    for (Vector3i records : explosion.getRecords()) {
                        player.compensatedWorld.updateBlock(records.x, records.y, records.z, 0);
                    }
                });
            }

            if (velocity.x != 0 || velocity.y != 0 || velocity.z != 0) {
                // No need to spam transactions
                if (explosion.getRecords().isEmpty()) player.sendTransaction();
                addPlayerExplosion(player.lastTransactionSent.get(), velocity);
                event.getPostTasks().add(player::sendTransaction);
            }
        }
    }

    public void addPlayerExplosion(int breadOne, Vector3f explosion) {
        firstBreadMap.add(new VelocityData(-1, breadOne, player.getSetbackTeleportUtil().isSendingSetback, new Vector(explosion.getX(), explosion.getY(), explosion.getZ())));
    }

    public void setPointThree(boolean isPointThree) {
        wasKbZeroPointZeroThree = wasKbZeroPointZeroThree || isPointThree;
    }

    public void handlePredictionAnalysis(double offset) {
        if (player.firstBreadExplosion != null) {
            player.firstBreadExplosion.offset = Math.min(player.firstBreadExplosion.offset, offset);
        }

        if (player.likelyExplosions != null) {
            player.likelyExplosions.offset = Math.min(player.likelyExplosions.offset, offset);
        }
    }

    public void onTeleport() {
        if (player.getSetbackTeleportUtil().getRequiredSetBack() == null ||
                player.getSetbackTeleportUtil().getRequiredSetBack().isPlugin()) {
            forceExempt();
        }
    }

    public void forceExempt() {
        // Don't exempt if the player used grim to get a teleport here.
        // This will flag but it's required to stop abuse
        if (player.getSetbackTeleportUtil().getRequiredSetBack() == null ||
                player.getSetbackTeleportUtil().getRequiredSetBack().isPlugin()) {
            // Unsure explosion was taken
            if (player.firstBreadExplosion != null) {
                player.firstBreadExplosion.offset = 0;
            }

            if (player.likelyExplosions != null) {
                player.likelyExplosions.offset = 0;
            }
        }
    }

    public void handlePlayerExplosion(double offset) {
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

        if (!wasZero && player.predictedVelocity.isKnockback() && player.likelyExplosions == null && player.firstBreadExplosion != null) {
            // The player took this knockback, this tick, 100%
            // Fixes exploit that would allow players to take explosions an infinite number of times
            if (player.firstBreadExplosion.offset < offsetToFlag) {
                firstBreadAddedExplosion = null;
            }
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
        if (player.likelyExplosions != null) {
            if (player.likelyExplosions.offset > offsetToFlag) {
                if (flag()) {
                    if (getViolations() > setbackVL) {
                        player.getSetbackTeleportUtil().executeViolationSetback(!player.likelyExplosions.hasSetbackForThis);
                    }
                }

                String formatOffset = "o: " + formatOffset(offset);

                if (player.likelyExplosions.offset == Integer.MAX_VALUE) {
                    formatOffset = "ignored explosion";
                }

                alert(formatOffset);
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
                firstBreadMap.poll();
                if (lastExplosionsKnownTaken != null)
                    firstBreadAddedExplosion = new VelocityData(-1, data.transaction, data.isSetback, lastExplosionsKnownTaken.vector.clone().add(data.vector));
                else
                    firstBreadAddedExplosion = new VelocityData(-1, data.transaction, data.isSetback, data.vector);
                break; // All knockback after this will have not been applied
            } else if (data.transaction < transactionID) {
                if (lastExplosionsKnownTaken != null)
                    lastExplosionsKnownTaken.vector.clone().add(data.vector);
                else {
                    if (firstBreadAddedExplosion != null) // Bring over the previous offset, don't require explosions twice
                        lastExplosionsKnownTaken = new VelocityData(-1, data.transaction, data.vector, data.isSetback, firstBreadAddedExplosion.offset);
                    else
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

    public VelocityData getFirstBreadAddedExplosion(int lastTransaction) {
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
