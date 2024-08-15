package ac.grim.grimac.checks.impl.movement;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;

@CheckData(name = "NoSlowC", setback = 5, experimental = true)
public class NoSlowC extends Check implements PostPredictionCheck, PacketCheck {
    public NoSlowC(GrimPlayer player) {
        super(player);
    }

    public boolean startedSprintingBeforeSlowMovement = false;

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.ENTITY_ACTION) {
            if (new WrapperPlayClientEntityAction(event).getAction() == WrapperPlayClientEntityAction.Action.START_SPRINTING) {
                startedSprintingBeforeSlowMovement = false;
            }
        }
    }

    @Override
    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        if (!predictionComplete.isChecked()) return;

        if (player.isSlowMovement) {
            ClientVersion client = player.getClientVersion();

            // https://bugs.mojang.com/browse/MC-152728
            if (startedSprintingBeforeSlowMovement && client.isNewerThanOrEquals(ClientVersion.V_1_14_2)) {
                reward();
                return;
            }

            if (player.isSprinting
                    // you can sneak and swim in 1.13 - 1.14.1
                    && (!player.isSwimming || client.isNewerThan(ClientVersion.V_1_14_1) || client.isOlderThan(ClientVersion.V_1_13))
                    && player.sneakingSpeedMultiplier < 0.8f
            ) {
                if (flagWithSetback()) alert("");
            } else reward();
        }
    }
}
