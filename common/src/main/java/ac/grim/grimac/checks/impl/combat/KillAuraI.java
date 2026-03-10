package ac.grim.grimac.checks.impl.combat;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;

@CheckData(name = "KillAuraI", description = "Attack bursts with no movement packets between", decay = 0.02, experimental = true)
public class KillAuraI extends Check implements PostPredictionCheck {
    private int attacksWithoutMove;

    public KillAuraI(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity interact = new WrapperPlayClientInteractEntity(event);
            if (interact.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK && ++attacksWithoutMove > 3) {
                flagAndAlert("count=" + attacksWithoutMove);
            }
            return;
        }

        if (isTickPacket(event.getPacketType())) {
            attacksWithoutMove = 0;
            reward();
        }
    }
}
