package ac.grim.grimac.checks.impl.combat;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;

@CheckData(name = "KillAuraF", description = "Attack packets before any rotation in the tick", decay = 0.02, experimental = true)
public class KillAuraF extends Check implements PostPredictionCheck {
    private boolean sawRotation;
    private int streak;

    public KillAuraF(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_ROTATION
                || event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
            sawRotation = true;
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity interact = new WrapperPlayClientInteractEntity(event);
            if (interact.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK && !sawRotation) {
                if (++streak > 7) {
                    flagAndAlert("streak=" + streak);
                }
            }
        }

        if (isTickPacket(event.getPacketType())) {
            sawRotation = false;
            streak = Math.max(0, streak - 1);
            reward();
        }
    }
}
