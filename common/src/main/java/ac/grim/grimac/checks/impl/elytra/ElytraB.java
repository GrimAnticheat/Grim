package ac.grim.grimac.checks.impl.elytra;

import ac.grim.grimac.api.storage.verbose.VerboseSchema;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;

@CheckData(name = "ElytraB", stableKey = "grim.elytra.no_jump", description = "Started gliding without jumping", verboseVersion = 1)
public class ElytraB extends Check implements PostPredictionCheck {
    public static final VerboseSchema V = VerboseSchema.of("release:bool");

    private boolean glide;
    private boolean setback;

    public ElytraB(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.ENTITY_ACTION
                && new WrapperPlayClientEntityAction(event).getAction() == WrapperPlayClientEntityAction.Action.START_FLYING_WITH_ELYTRA
                && player.supportsEndTick()
        ) {
            if (player.packetStateData.knownInput.jump()) {
                if (flagAndAlert(V.write(verbose()).bool(true))) {
                    setback = true;
                    if (shouldModifyPackets()) {
                        event.setCancelled(true);
                        player.onPacketCancel();
                        player.resyncPose();
                    }
                }
            } else {
                glide = true;
            }
        }

        if (isUpdate(event.getPacketType())) {
            if (glide && !player.packetStateData.knownInput.jump() && flagAndAlert(V.write(verbose()).bool(false))) {
                setback = true;
            }

            glide = false;
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete predictionComplete) {
        if (setback) {
            setback = false;
            setbackIfAboveSetbackVL();
        }
    }
}
