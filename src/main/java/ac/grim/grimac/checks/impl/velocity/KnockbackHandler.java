package ac.grim.grimac.checks.impl.velocity;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import ac.grim.grimac.utils.data.Pair;
import ac.grim.grimac.utils.data.VectorData;
import ac.grim.grimac.utils.data.VelocityData;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityVelocity;
import lombok.Getter;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.Deque;
import java.util.LinkedList;

// We are making a velocity sandwich between two pieces of transaction packets (bread)
@CheckData(name = "AntiKB", alternativeName = "AntiKnockback", configName = "Knockback", setback = 10, decay = 0.025)
public class KnockbackHandler extends Check implements PostPredictionCheck {
    Deque<VelocityData> firstBreadMap = new LinkedList<>();

    Deque<VelocityData> lastKnockbackKnownTaken = new LinkedList<>();
    VelocityData firstBreadOnlyKnockback = null;
    @Getter
    boolean knockbackPointThree = false;

    double offsetToFlag;
    double maxAdv, immediate, ceiling, multiplier;

    double threshold;

    public KnockbackHandler(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketSend(final PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.ENTITY_VELOCITY) {
            WrapperPlayServerEntityVelocity velocity = new WrapperPlayServerEntityVelocity(event);
            int entityId = velocity.getEntityId();

            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;

            // Detect whether this knockback packet affects the player or if it is useless
            // Mojang sends extra useless knockback packets for no apparent reason
            if (player.compensatedEntities.serverPlayerVehicle != null && entityId != player.compensatedEntities.serverPlayerVehicle) {
                return;
            }
            if (player.compensatedEntities.serverPlayerVehicle == null && entityId != player.entityID) {
                return;
            }

            // If the player isn't in a vehicle and the ID is for the player, the player will take kb
            // If the player is in a vehicle and the ID is for the player's vehicle, the player will take kb
            Vector3d playerVelocity = velocity.getVelocity();

            // Wrap velocity between two transactions
            player.sendTransaction();
            addPlayerKnockback(entityId, player.lastTransactionSent.get(), new Vector(playerVelocity.getX(), playerVelocity.getY(), playerVelocity.getZ()));
            event.getTasksAfterSend().add(player::sendTransaction);
        }
    }

    @NotNull public Pair<VelocityData, Vector> getFutureKnockback() {
        // Chronologically in the future
        if (firstBreadMap.size() > 0) {
            VelocityData data = firstBreadMap.peek();
            return new Pair<>(data, data != null ? data.vector : null);
        }
        // Less in the future
        if (lastKnockbackKnownTaken.size() > 0) {
            VelocityData data = lastKnockbackKnownTaken.peek();
            return new Pair<>(data, data != null ? data.vector : null);
        }
        // Uncertain, might be in the future
        if (player.firstBreadKB != null && player.likelyKB == null) {
            VelocityData data = player.firstBreadKB;
            return new Pair<>(data, data.vector.clone());
        } else if (player.likelyKB != null) { // Known to be in the present
            VelocityData data = player.likelyKB;
            return new Pair<>(data, data.vector.clone());
        }
        return new Pair<>(null, null);
    }

    private void addPlayerKnockback(int entityID, int breadOne, Vector knockback) {
        firstBreadMap.add(new VelocityData(entityID, breadOne, player.getSetbackTeleportUtil().isSendingSetback, knockback));
    }

    public VelocityData calculateRequiredKB(int entityID, int transaction, boolean isJustTesting) {
        tickKnockback(transaction);

        VelocityData returnLastKB = null;
        for (VelocityData data : lastKnockbackKnownTaken) {
            if (data.entityID == entityID)
                returnLastKB = data;
        }

        if (!isJustTesting) {
            lastKnockbackKnownTaken.clear();
        }
        return returnLastKB;
    }

    private void tickKnockback(int transactionID) {
        firstBreadOnlyKnockback = null;
        if (firstBreadMap.isEmpty()) return;
        VelocityData data = firstBreadMap.peek();
        while (data != null) {
            if (data.transaction == transactionID) { // First bread knockback
                firstBreadOnlyKnockback = new VelocityData(data.entityID, data.transaction, data.isSetback, data.vector);
                //firstBreadMap.poll();
                break; // All knockback after this will have not been applied
            } else if (data.transaction < transactionID) { // This kb has 100% arrived to the player
                if (firstBreadOnlyKnockback != null) // Don't require kb twice
                    lastKnockbackKnownTaken.add(new VelocityData(data.entityID, data.transaction, data.vector, data.isSetback, data.offset));
                else
                    lastKnockbackKnownTaken.add(new VelocityData(data.entityID, data.transaction, data.isSetback, data.vector));
                firstBreadOnlyKnockback = null;
                firstBreadMap.poll();
                data = firstBreadMap.peek();
            } else { // We are too far ahead in the future
                break;
            }
        }
    }

    public void forceExempt() {
        // Unsure knockback was taken
        if (player.firstBreadKB != null) {
            player.firstBreadKB.offset = 0;
        }

        if (player.likelyKB != null) {
            player.likelyKB.offset = 0;
        }
    }

    public void setPointThree(boolean isPointThree) {
        knockbackPointThree = knockbackPointThree || isPointThree;
    }

    public void handlePredictionAnalysis(double offset) {
        if (player.firstBreadKB != null) {
            player.firstBreadKB.offset = Math.min(player.firstBreadKB.offset, offset);
        }

        if (player.likelyKB != null) {
            player.likelyKB.offset = Math.min(player.likelyKB.offset, offset);
        }
    }

    @Override
    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        double offset = predictionComplete.getOffset();
        if (!predictionComplete.isChecked() || predictionComplete.getData().isTeleport()) {
            forceExempt();
            return;
        }

        boolean wasZero = knockbackPointThree;
        knockbackPointThree = false;

        if (player.likelyKB == null && player.firstBreadKB == null) {
            return;
        }

        if (player.predictedVelocity.isFirstBreadKb()) {
            firstBreadOnlyKnockback = null;
            firstBreadMap.poll(); // Remove from map so we don't pull it again
        }

        if (wasZero || player.predictedVelocity.isKnockback()) {
            // Unsure knockback was taken
            if (player.firstBreadKB != null) {
                player.firstBreadKB.offset = Math.min(player.firstBreadKB.offset, offset);
            }

            // 100% known kb was taken
            if (player.likelyKB != null) {
                player.likelyKB.offset = Math.min(player.likelyKB.offset, offset);
            }
        }

        if (player.likelyKB != null) {
            if (player.likelyKB.offset > offsetToFlag) {
                threshold = Math.min(threshold + player.likelyKB.offset, ceiling);
                if (player.likelyKB.isSetback) { // Don't increase violations if this velocity was setback, just teleport and resend them velocity.
                    player.getSetbackTeleportUtil().executeViolationSetback();
                } else if (flag()) { // This velocity was sent by the server.
                    if (player.likelyKB.offset >= immediate || threshold >= maxAdv) {
                        player.getSetbackTeleportUtil().executeViolationSetback();
                    }

                    String formatOffset = "o: " + formatOffset(player.likelyKB.offset);

                    if (player.likelyKB.offset == Integer.MAX_VALUE) {
                        formatOffset = "ignored knockback";
                    }

                    alert(formatOffset);
                } else {
                    reward();
                }
            } else if (threshold > 0.05) {
                threshold *= multiplier;
            }
        }
    }

    public boolean shouldIgnoreForPrediction(VectorData data) {
        if (data.isKnockback() && data.isFirstBreadKb()) {
            return player.firstBreadKB.offset > offsetToFlag;
        }
        return false;
    }

    public boolean wouldFlag() {
        return (player.likelyKB != null && player.likelyKB.offset > offsetToFlag) || (player.firstBreadKB != null && player.firstBreadKB.offset > offsetToFlag);
    }

    public VelocityData calculateFirstBreadKnockback(int entityID, int transaction) {
        tickKnockback(transaction);
        if (firstBreadOnlyKnockback != null && firstBreadOnlyKnockback.entityID == entityID)
            return firstBreadOnlyKnockback;
        return null;
    }

    @Override
    public void reload() {
        super.reload();
        offsetToFlag = getConfig().getDoubleElse("Knockback.threshold", 0.001);
        maxAdv = getConfig().getDoubleElse("Knockback.max-advantage", 1);
        immediate = getConfig().getDoubleElse("Knockback.immediate-setback-threshold", 0.1);
        multiplier = getConfig().getDoubleElse("Knockback.setback-decay-multiplier", 0.999);
        ceiling = getConfig().getDoubleElse("Knockback.max-ceiling", 4);

        if (maxAdv < 0) maxAdv = Double.MAX_VALUE;
        if (immediate < 0) immediate = Double.MAX_VALUE;
    }
}
