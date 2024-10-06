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

    private boolean sprint;
    private boolean sneak;
    private int flags = 0;

    @Override
    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8)) {
            if (flags > 0) {
                setbackIfAboveSetbackVL();
            }

            flags = 0;
            return;
        }

        if (!player.skippedTickInActualMovement && predictionComplete.isChecked() {
            for (; flags > 0; flags--) {
                if (flagAndAlert()) {
                    setbackIfAboveSetbackVL();
                }
            }
        }

        sprint = sneak = false;
        flags = 0;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType()) && player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8) && !player.packetStateData.lastPacketWasTeleport) {
            sprint = sneak = false;
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.ENTITY_ACTION) {
            if (player.gamemode == GameMode.SPECTATOR) return; // you don't send flying packets when spectating entities

            switch (new WrapperPlayClientEntityAction(event).getAction()) {
                case START_SNEAKING:
                case STOP_SNEAKING:
                    if (sneak) {
                        if (player.getClientVersion().isNewerThan(ClientVersion.V_1_8) || flagAndAlert()) {
                            flags++;
                        }
                    }

                    sneak = true;
                    break;
                case START_SPRINTING:
                case STOP_SPRINTING:
                    if (sprint) {
                        if (player.getClientVersion().isNewerThan(ClientVersion.V_1_8) || flagAndAlert()) {
                            flags++;
                        }
                    }

                    sprint = true;
                    break;
            }
        }
    }
}
