package ac.grim.grimac.checks.impl.elytra;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;

@CheckData(name = "ElytraC", description = "Started gliding too frequently", experimental = true)
public class ElytraC extends Check implements PostPredictionCheck {
    private boolean glideThisTick, glideLastTick, setback;
    private int flags;

    public ElytraC(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8)) {
            return;
        }

        if (player.gamemode == GameMode.SPECTATOR) {
            glideThisTick = glideLastTick = false;
        }

        if (event.getPacketType() == PacketType.Play.Client.ENTITY_ACTION && new WrapperPlayClientEntityAction(event).getAction() == WrapperPlayClientEntityAction.Action.START_FLYING_WITH_ELYTRA) {
            if (glideThisTick || glideLastTick) {
                if (player.canSkipTicks()) {
                    flags++;
                } else {
                    if (flagAndAlert()) {
                        setback = true;
                        if (shouldModifyPackets()) {
                            event.setCancelled(true);
                            player.onPacketCancel();
                            player.resyncPose();
                        }
                    }
                }
            }

            glideThisTick = true;
        }

        if (isTickPacket(event.getPacketType())) {
            glideLastTick = glideThisTick;
            glideThisTick = false;
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete predictionComplete) {
        if (player.canSkipTicks()) {
            if (player.isTickingReliablyFor(3)) {
                for (; flags > 0; flags--) {
                    flagAndAlert();
                }
            }

            flags = 0;
            setback = false;
        }

        if (setback) {
            setback = false;
            setbackIfAboveSetbackVL();
        }
    }
}
