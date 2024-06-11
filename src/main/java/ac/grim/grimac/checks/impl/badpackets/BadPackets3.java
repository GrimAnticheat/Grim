package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "BadPackets3", experimental = true)
public class BadPackets3 extends Check implements PacketCheck, PostPredictionCheck {
    public BadPackets3(GrimPlayer player) {
        super(player);
    }

    private float sprints = 0;
    private float sneaks = 0;

    @Override
    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        // we don't need to check pre-1.9 players here (no tick skipping)
        if (player.getClientVersion().isOlderThan(ClientVersion.V_1_9)) return;

        if (player.skippedTickInActualMovement) {
            sprints *= 0.05f;
            sneaks *= 0.05f;
        }

        for (; sneaks > 1; sneaks--) flagAndAlert("sneak");
        for (; sprints > 1; sprints--) flagAndAlert("sprint");
        sneaks = 0;
        sprints = 0;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType()) && player.getClientVersion().isOlderThan(ClientVersion.V_1_9) && !player.packetStateData.lastPacketWasTeleport) {
            sprints = 0;
            sneaks = 0;
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.ENTITY_ACTION) {

            // the client does not send flying packets when spectating entities
            if (player.gamemode == GameMode.SPECTATOR) {
                return;
            }

            switch (new WrapperPlayClientEntityAction(event).getAction()) {
                case START_SNEAKING:
                case STOP_SNEAKING:
                    sneaks++;
                    if (player.getClientVersion().isOlderThan(ClientVersion.V_1_9) && sneaks > 1) {
                        if (flagAndAlert("sneak") && shouldModifyPackets()) {
                            event.setCancelled(true);
                            player.onPacketCancel();
                        }
                    }
                    break;
                case START_SPRINTING:
                case STOP_SPRINTING:
                    sprints++;
                    if (player.getClientVersion().isOlderThan(ClientVersion.V_1_9) && sprints > 1) {
                        if (flagAndAlert("sprint") && shouldModifyPackets()) {
                            event.setCancelled(true);
                            player.onPacketCancel();
                        }
                    }
                    break;
            }
        }
    }
}
