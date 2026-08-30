package ac.grim.grimac.checks.impl.elytra;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketReceiveListener;
import ac.grim.grimac.checks.type.PostPredictionListener;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;

@CheckData(name = "ElytraF", stableKey = "grim.elytra.grounded", description = "Started gliding while on ground")
public class ElytraF extends Check implements PacketReceiveListener, PostPredictionListener {
    private boolean setback;

    public ElytraF(GrimPlayer player) {
        super(player);
    }

    @Override
    public boolean isApplicable() {
        return player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.ENTITY_ACTION
                && new WrapperPlayClientEntityAction(event).getAction() == WrapperPlayClientEntityAction.Action.START_FLYING_WITH_ELYTRA
                && player.clientClaimsLastOnGround
                && flag()) {
            setback = true;
            if (shouldModifyPackets()) {
                event.setCancelled(true);
                player.onPacketCancel();
                player.resyncGlidingState();
            }
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete predictionComplete) {
        if (setback) {
            setbackIfAboveSetbackVL();
            setback = false;
        }
    }
}
