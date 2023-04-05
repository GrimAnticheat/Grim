package ac.grim.grimac.checks.impl.inventory;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import ac.grim.grimac.utils.data.VectorData;
import ac.grim.grimac.utils.data.VectorData.MoveVectorData;
import ac.grim.grimac.utils.data.VehicleData;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientCloseWindow;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerCloseWindow;
import java.util.StringJoiner;

@CheckData(name = "InventoryD", setback = 1, decay = 0.25)
public class InventoryD extends Check implements PostPredictionCheck {

    // Impossible transaction ID
    private static final long NONE = Long.MAX_VALUE;

    private long closeTransaction = NONE;
    private int сlosePacketsToSkip;

    private int horseJumpVerbose;

    public InventoryD(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {
            // Disallow any clicks if inventory is closing
            if(closeTransaction != NONE && shouldModifyPackets()) {
                event.setCancelled(true);
                player.onPacketCancel();
                player.getInventory().needResend = true;
            }
        } else if (event.getPacketType() == PacketType.Play.Client.CLOSE_WINDOW) {
            // Players with high ping can close inventory faster than send transaction back
            if (closeTransaction != NONE && сlosePacketsToSkip-- <= 0) {
                closeTransaction = NONE;
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

                // Will flag once if player open anything with pressed space bar
                isJumping = vehicle.nextHorseJump > 0 && horseJumpVerbose++ >= 1;
                isMoving = vehicle.nextVehicleForward != 0 || vehicle.nextVehicleHorizontal != 0;
            } else {
                MoveVectorData move = findMovement(player.predictedVelocity);

                isJumping = player.predictedVelocity.isJump();
                isMoving = move != null && (move.x != 0 || move.z != 0);
            }

            if (!isMoving && !isJumping) {
                reward();
                return;
            }

            if (flagWithSetback()) {
                closeInventory();

                StringJoiner joiner = new StringJoiner(" ");

                if (isMoving) joiner.add("moving");
                if (isJumping) joiner.add("jumping");
                if (inVehicle) joiner.add("inVehicle");

                alert(joiner.toString());
            }
        } else {
            horseJumpVerbose = 0;
        }
    }

    public void closeInventory() {
        if(closeTransaction != NONE) {
            return;
        }

        int windowId = player.getInventory().openWindowID;

        player.user.writePacket(new WrapperPlayServerCloseWindow(windowId));

        // Force close inventory on server side
        сlosePacketsToSkip = 1; // Sending close packet to itself, so skip it
        PacketEvents.getAPI().getProtocolManager().receivePacket(
           player.user.getChannel(), new WrapperPlayClientCloseWindow(windowId)
        );

        player.sendTransaction();

        int transaction = player.lastTransactionSent.get();
        closeTransaction = transaction;
        player.latencyUtils.addRealTimeTask(transaction, () -> {
            if (closeTransaction == transaction) {
                closeTransaction = NONE;
            }
        });

        player.user.flushPackets();
    }

    private MoveVectorData findMovement(VectorData vectorData) {
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
