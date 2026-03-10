package ac.grim.grimac.checks.impl.combat;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;

@CheckData(name = "KillAuraE", description = "Repeated attacks with stale look packets", decay = 0.02, experimental = true)
public class KillAuraE extends Check implements PostPredictionCheck {
    private int ticksSinceLook;
    private int staleAttackStreak;

    public KillAuraE(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_ROTATION
                || event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
            ticksSinceLook = 0;
            staleAttackStreak = Math.max(0, staleAttackStreak - 1);
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity interact = new WrapperPlayClientInteractEntity(event);
            if (interact.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK && ticksSinceLook > 2) {
                if (++staleAttackStreak > 5) {
                    flagAndAlert("ticks=" + ticksSinceLook + ", streak=" + staleAttackStreak);
                }
            }
        }

        if (isTickPacket(event.getPacketType())) {
            ticksSinceLook++;
        }
    }
}
