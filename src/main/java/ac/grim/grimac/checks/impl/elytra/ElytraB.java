package ac.grim.grimac.checks.impl.elytra;

import ac.grim.grimac.checks.AbstractPacketCheck;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.PacketHandlerRegistry;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;

@CheckData(name = "ElytraB", description = "Started gliding without jumping", experimental = true)
public class ElytraB extends AbstractPacketCheck implements PostPredictionCheck {
    private boolean glide;
    private boolean setback;

    public ElytraB(GrimPlayer player) {
        super(player);
    }

    @Override
    protected void registerReceiveHandlers(PacketHandlerRegistry<PacketReceiveEvent> registry) {
        registry.registerHandler(event -> {
            if (new WrapperPlayClientEntityAction(event).getAction() == WrapperPlayClientEntityAction.Action.START_FLYING_WITH_ELYTRA
                    && player.supportsEndTick()
            ) {
                if (player.packetStateData.knownInput.jump()) {
                    if (flagAndAlert("no release")) {
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
        }, PacketType.Play.Client.ENTITY_ACTION);
        registry.registerHandler(event -> {
            if (glide && !player.packetStateData.knownInput.jump() && flagAndAlert("no jump")) {
                setback = true;
            }

            glide = false;
        }, this::isUpdate);
    }

    @Override
    public void onPredictionComplete(PredictionComplete predictionComplete) {
        if (setback) {
            setback = false;
            setbackIfAboveSetbackVL();
        }
    }
}
