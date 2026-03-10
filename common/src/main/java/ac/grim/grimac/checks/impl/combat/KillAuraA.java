package ac.grim.grimac.checks.impl.combat;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;

@CheckData(name = "KillAuraA", description = "Attacking without arm animation", decay = 0.02, experimental = true)
public class KillAuraA extends Check implements PostPredictionCheck {
    private boolean attacked;
    private boolean animated;

    public KillAuraA(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.ANIMATION) {
            animated = true;
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity interact = new WrapperPlayClientInteractEntity(event);
            if (interact.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                attacked = true;
            }
        }

        if (isTickPacket(event.getPacketType())) {
            if (attacked && !animated && flagAndAlert()) {
                reward();
            }

            attacked = false;
            animated = false;
        }
    }
}
