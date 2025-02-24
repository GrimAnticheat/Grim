package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;

@CheckData(name = "BadPacketsX", experimental = true)
public class BadPacketsX extends Check implements PostPredictionCheck {
    public BadPacketsX(GrimPlayer player) {
        super(player);
    }

    private boolean sprint;
    private boolean sneak;
    private int flags;

    @Override
    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        if (!player.canSkipTicks()) {
            if (flags > 0) {
                setbackIfAboveSetbackVL();
            }

            flags = 0;
            return;
        }

        if (player.isTickingReliablyFor(3)) {
            for (; flags > 0; flags--) {
                flagAndAlertWithSetback();
            }
        }

        flags = 0;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.gamemode == GameMode.SPECTATOR || isTickPacket(event.getPacketType())) {
            sprint = sneak = false;
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.ENTITY_ACTION) {
            switch (new WrapperPlayClientEntityAction(event).getAction()) {
                case START_SNEAKING, STOP_SNEAKING -> {
                    if (sneak) {
                        if (player.canSkipTicks() || flagAndAlert()) {
                            flags++;
                        }
                    }

                    sneak = true;
                }
                case START_SPRINTING, STOP_SPRINTING -> {
                    if (player.inVehicle()) {
                        return;
                    }

                    if (sprint) {
                        if (player.canSkipTicks() || flagAndAlert()) {
                            flags++;
                        }
                    }

                    sprint = true;
                }
            }
        }
    }
}
