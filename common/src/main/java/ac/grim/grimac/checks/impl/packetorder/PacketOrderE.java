package ac.grim.grimac.checks.impl.packetorder;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;

import java.util.ArrayDeque;

@CheckData(name = "PacketOrderE", experimental = true)
public class PacketOrderE extends Check implements PostPredictionCheck {
    public PacketOrderE(final GrimPlayer player) {
        super(player);
    }

    private final ArrayDeque<String> flags = new ArrayDeque<>();
    private boolean setback;

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.HELD_ITEM_CHANGE) {
            if (player.packetOrderProcessor.isAttacking()
                    || player.packetOrderProcessor.isRightClicking()
                    || player.packetOrderProcessor.isOpeningInventory()
                    || player.packetOrderProcessor.isReleasing()
                    || player.packetOrderProcessor.isSneaking()
                    || player.packetOrderProcessor.isSprinting()
                    || player.packetOrderProcessor.isLeavingBed()
                    || player.packetOrderProcessor.isStartingToGlide()
                    || player.packetOrderProcessor.isJumpingWithMount()
            ) {
                String verbose = "attacking=" + player.packetOrderProcessor.isAttacking()
                        + ", rightClicking=" + player.packetOrderProcessor.isRightClicking()
                        + ", openingInventory=" + player.packetOrderProcessor.isOpeningInventory()
                        + ", releasing=" + player.packetOrderProcessor.isReleasing()
                        + ", sneaking=" + player.packetOrderProcessor.isSneaking()
                        + ", sprinting=" + player.packetOrderProcessor.isSprinting()
                        + ", bed=" + player.packetOrderProcessor.isLeavingBed()
                        + ", sprinting=" + player.packetOrderProcessor.isSprinting()
                        + ", gliding=" + player.packetOrderProcessor.isStartingToGlide()
                        + ", mountJumping=" + player.packetOrderProcessor.isJumpingWithMount();
                if (player.canSkipTicks() && flags.add(verbose) || flagAndAlert(verbose)) {
                    if (player.packetOrderProcessor.isUsing()) {
                        setback = true;
                    }
                }
            }
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete predictionComplete) {
        if (!player.canSkipTicks()) {
            if (setback) {
                setback = false;
                setbackIfAboveSetbackVL();
            }
            return;
        }

        if (player.isTickingReliablyFor(3)) {
            for (String verbose : flags) {
                if (flagAndAlert(verbose) && setback) {
                    setback = false;
                    setbackIfAboveSetbackVL();
                }
            }
        }

        setback = false;
        flags.clear();
    }
}
