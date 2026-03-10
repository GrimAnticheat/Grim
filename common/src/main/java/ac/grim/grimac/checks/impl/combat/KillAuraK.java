package ac.grim.grimac.checks.impl.combat;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;

@CheckData(name = "KillAuraK", description = "Attacking while look stays exactly frozen", decay = 0.02, experimental = true)
public class KillAuraK extends Check implements PostPredictionCheck {
    private float lastYaw;
    private float lastPitch;
    private int frozenAttackStreak;

    public KillAuraK(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) {
            return;
        }

        WrapperPlayClientInteractEntity interact = new WrapperPlayClientInteractEntity(event);
        if (interact.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
            return;
        }

        if (Math.abs(player.yaw - lastYaw) < 1.0E-5F && Math.abs(player.pitch - lastPitch) < 1.0E-5F) {
            if (++frozenAttackStreak > 8) {
                flagAndAlert("streak=" + frozenAttackStreak);
            }
        } else {
            frozenAttackStreak = Math.max(0, frozenAttackStreak - 1);
            reward();
        }

        lastYaw = player.yaw;
        lastPitch = player.pitch;
    }
}
