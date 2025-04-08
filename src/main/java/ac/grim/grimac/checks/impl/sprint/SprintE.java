package ac.grim.grimac.checks.impl.sprint;

import ac.grim.grimac.checks.AbstractPacketCheck;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.PacketHandlerRegistry;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;

@CheckData(name = "SprintE", description = "Sprinting while colliding with a wall", setback = 5, experimental = true)
public class SprintE extends AbstractPacketCheck implements PostPredictionCheck {
    public SprintE(GrimPlayer player) {
        super(player);
    }

    private boolean startedSprintingThisTick, wasHorizontalCollision;

    @Override
    protected void registerReceiveHandlers(PacketHandlerRegistry<PacketReceiveEvent> registry) {
        registry.registerHandler(event -> {
            if (new WrapperPlayClientEntityAction(event).getAction() == WrapperPlayClientEntityAction.Action.START_SPRINTING) {
                startedSprintingThisTick = true;
            }
        }, PacketType.Play.Client.ENTITY_ACTION);
    }

    @Override
    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        if (!predictionComplete.isChecked()) return;

        // there's a mechanic in 1.18+ that allows this if you are looking far enough away from the wall
        // I'll probably check 1.18+ later
        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_18)) return;

        if (wasHorizontalCollision && !startedSprintingThisTick && (!player.wasTouchingWater || player.getClientVersion().isOlderThan(ClientVersion.V_1_13))) {
            if (player.isSprinting) {
                flagAndAlertWithSetback();
            } else reward();
        }

        wasHorizontalCollision = player.horizontalCollision;
        startedSprintingThisTick = false;
    }
}
