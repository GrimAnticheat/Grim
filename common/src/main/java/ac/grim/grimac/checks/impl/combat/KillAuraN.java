package ac.grim.grimac.checks.impl.combat;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;

@CheckData(name = "KillAuraN", description = "Attack streaks during zero pitch updates", decay = 0.02, experimental = true)
public class KillAuraN extends Check implements PostPredictionCheck {
    private float lastPitch;
    private int zeroPitchAttackStreak;

    public KillAuraN(GrimPlayer player) {
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

        if (Math.abs(player.pitch - lastPitch) < 1.0E-5F) {
            if (++zeroPitchAttackStreak > 10) {
                flagAndAlert("streak=" + zeroPitchAttackStreak);
            }
        } else {
            zeroPitchAttackStreak = Math.max(0, zeroPitchAttackStreak - 1);
            reward();
        }

        lastPitch = player.pitch;
    }
}
