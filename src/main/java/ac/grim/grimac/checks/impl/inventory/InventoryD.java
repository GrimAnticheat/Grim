package ac.grim.grimac.checks.impl.inventory;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import ac.grim.grimac.utils.data.VectorData;
import ac.grim.grimac.utils.data.VectorData.MoveVectorData;
import ac.grim.grimac.utils.data.VehicleData;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import java.util.StringJoiner;

// TODO: Works well with 0.0002, but not with 0.03. Should find a way to properly determine inputs from 0.03.
@CheckData(name = "InventoryD", setback = 1, decay = 0.25)
public class InventoryD extends Check implements PostPredictionCheck {
    private int controlledMovements;
    private int horseJumpVerbose;

    public InventoryD(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {
            // TODO: this value is unreasonable high, reduce when this check will be 100% stable
            if (controlledMovements >= 2) {
                if (shouldModifyPackets()) {
                    event.setCancelled(true);
                    player.onPacketCancel();
                }

                flagAndAlert("inventory click");
            }
        }
    }

    @Override
    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        if (!predictionComplete.isChecked() ||
                predictionComplete.getData().isTeleport() ||
                player.getSetbackTeleportUtil().blockOffsets) {
            return;
        }

        if (player.hasInventoryOpen) {
            boolean inVehicle = player.compensatedEntities.getSelf().inVehicle();
            boolean isJumping, isMoving;

            if (inVehicle) {
                VehicleData vehicle = player.vehicleData;

                // Will flag once, if player opens chest with pressed space bar
                isJumping = vehicle.nextHorseJump > 0 && horseJumpVerbose++ >= 1;
                isMoving = vehicle.nextVehicleForward != 0 || vehicle.nextVehicleHorizontal != 0;
            } else {
                isJumping = player.predictedVelocity.isJump() &&
                            // TODO: pistons.
                            !player.uncertaintyHandler.slimePistonBounces.contains(BlockFace.UP) &&
                            // TODO: investigate why swimming players flagging this
                            // TODO: maybe setbacks causing this?
                            !player.isSwimming;

                MoveVectorData move = findMovement(player.predictedVelocity);
                isMoving = move != null && (move.x != 0 || move.z != 0);
            }

            if (!isMoving && !isJumping) {
                controlledMovements = 0;
                reward();
                return;
            }

            controlledMovements++;

            // TODO: close inventory to prevent infinity setbacks
            if (flagWithSetback()) {
                StringJoiner joiner = new StringJoiner(" ");

                if (isMoving) joiner.add("moving");
                if (isJumping) joiner.add("jumping");
                if (inVehicle) joiner.add("inVehicle");

                alert(joiner.toString());
            }
        } else {
            horseJumpVerbose = 0;
            controlledMovements = 0;
        }
    }

    private MoveVectorData findMovement(VectorData vectorData) {
        if (!vectorData.isInputResult()) {
            return null;
        }

        if (vectorData instanceof MoveVectorData) {
            return (MoveVectorData) vectorData;
        }

        while (vectorData != null) {
            vectorData = vectorData.lastVector;
            if (vectorData instanceof MoveVectorData) {
                return (MoveVectorData) vectorData;
            }
        }

        return null;
    }
}
