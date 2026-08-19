package ac.grim.grimac.checks.impl.multiactions;

import ac.grim.grimac.api.storage.verbose.Verbose;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.impl.prediction.OffsetHandler;
import ac.grim.grimac.checks.type.BlockPlaceCheck;
import ac.grim.grimac.checks.type.PacketReceiveListener;
import ac.grim.grimac.checks.type.PostPredictionListener;
import ac.grim.grimac.checks.type.PreViaPacketReceiveListener;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import ac.grim.grimac.utils.latency.CompensatedOpenWindow;
import ac.grim.grimac.utils.math.Vector3dm;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import org.jetbrains.annotations.NotNull;

// TODO: test vehicles
// This can false in 1.8 due to the twitch button. I don't care; nobody uses it.
// Separate from MultiActionsC because that cancels the clicks, this can't.
@CheckData(name = "MultiActionsH", stableKey = "grim.multiactions.inventory_move", description = "Moving while in an inventory", experimental = true)
public class MultiActionsH extends BlockPlaceCheck implements PreViaPacketReceiveListener, PacketReceiveListener, PostPredictionListener {
    private static final Verbose V = Verbose.of("sprinting={bool}, sneaking={bool}, moving={bool}, jumping={bool}");

    public boolean vehicleJumping;

    public MultiActionsH(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPreViaPacketReceive(@NotNull PacketReceiveEvent event) {
        if (player.supportsEndTickPreVia() && event.getPacketType() == PacketType.Play.Client.CLIENT_TICK_END
                && player.openWindow.mustBeOpen()) {
            boolean sprinting = MultiActionsC.isVerboseSprinting(player);
            boolean moving = player.packetStateData.knownInput.movingNoJump();
            boolean jumping = player.packetStateData.knownInput.jump();

            if (!sprinting && !moving && !jumping) return;

            flag(V.write(verbose()).bool(sprinting).bool(false).bool(moving).bool(jumping));
        }
    }

    @Override
    public void onPacketReceive(@NotNull PacketReceiveEvent event) {
        if (player.supportsEndTickPreVia()) return;

        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            vehicleJumping = false;
        }

        if (!player.openWindow.mustBeOpen()) return;

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_ROTATION && !player.packetStateData.lastPacketWasTeleport
                || event.getPacketType() == PacketType.Play.Client.PLAYER_FLYING) {
            boolean sprinting = MultiActionsC.isVerboseSprinting(player);
            boolean sneaking = MultiActionsC.isVerboseSneaking(player);

            if (!sprinting && !sneaking) return;

            flag(V.write(verbose()).bool(sprinting).bool(sneaking).bool(false).bool(false));
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete predictionComplete) {
        if (player.supportsEndTickPreVia() || !player.openWindow.mustBeOpen()) {
            return;
        }

        boolean goodPrediction = !player.checkManager.getCheck(OffsetHandler.class).doesOffsetFlag(predictionComplete.getOffset())
                && predictionComplete.isChecked() || player.inVehicle();

        boolean sprinting = MultiActionsC.isVerboseSprinting(player);
        boolean sneaking = MultiActionsC.isVerboseSneaking(player);
        Vector3dm inputVec = player.predictedVelocity.input;
        boolean moving = goodPrediction && inputVec != null && !inputVec.isZero();
        boolean jumping = player.inVehicle() ? vehicleJumping : goodPrediction && player.predictedVelocity.isJump();

        if (!sprinting && !sneaking && !moving && !jumping) return;

        if (moving) {
            player.sendMessage("inventory type: " + player.openWindow.getPossibilities().stream().map(CompensatedOpenWindow.Window::type).toList());
            player.sendMessage("predicted input: " + MessageUtil.toUnlabledString(inputVec.toVector3d()));
        }

        flag(V.write(verbose()).bool(sprinting).bool(sneaking).bool(moving).bool(jumping));
    }
}
