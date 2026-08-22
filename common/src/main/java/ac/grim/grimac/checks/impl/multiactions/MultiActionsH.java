package ac.grim.grimac.checks.impl.multiactions;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.impl.prediction.OffsetHandler;
import ac.grim.grimac.checks.type.BlockPlaceCheck;
import ac.grim.grimac.checks.type.PostPredictionListener;
import ac.grim.grimac.checks.type.PreViaPacketReceiveListener;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import ac.grim.grimac.utils.math.Vector3dm;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import org.jetbrains.annotations.NotNull;

// This can false in 1.8 due to the twitch button. I don't care; nobody uses it.
// Separate from MultiActionsC because that cancels the clicks, this can't.
@CheckData(name = "MultiActionsH", stableKey = "grim.multiactions.inventory_move", description = "Moving while in an inventory", experimental = true)
public class MultiActionsH extends BlockPlaceCheck implements PreViaPacketReceiveListener, PostPredictionListener {

    public boolean vehicleJumping;

    public MultiActionsH(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPreViaPacketReceive(@NotNull PacketReceiveEvent event) {
        if (!player.supportsEndTickPreVia() || event.getPacketType() != PacketType.Play.Client.CLIENT_TICK_END
                || !player.openWindow.mustBeOpen()) return;

        if (player.packetStateData.knownInput.moving() && flag() && shouldModifyPackets()) {
            player.closeInventory();
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete predictionComplete) {
        if (player.supportsEndTickPreVia() || !player.openWindow.mustBeOpen()) {
            return;
        }

        boolean goodPrediction = !player.checkManager.getCheck(OffsetHandler.class).doesOffsetFlag(predictionComplete.getOffset())
                && predictionComplete.isChecked() || player.inVehicle();

        Vector3dm inputVec = player.predictedVelocity.input;
        boolean moving = goodPrediction && inputVec != null && !inputVec.isZero();
        boolean jumping = player.inVehicle() ? vehicleJumping : goodPrediction && player.predictedVelocity.isJump();

        if (!moving && !jumping) return;

        if (flag() && shouldModifyPackets()) {
            player.closeInventory();
        }
    }
}
