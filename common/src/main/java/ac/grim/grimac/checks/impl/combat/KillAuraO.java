package ac.grim.grimac.checks.impl.combat;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;

@CheckData(name = "KillAuraO", description = "High-frequency attack chains with minimal packet diversity", decay = 0.02, experimental = true)
public class KillAuraO extends Check implements PostPredictionCheck {
    private int attacks;
    private int nonAttackPackets;

    public KillAuraO(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (isTickPacket(event.getPacketType())) {
            if (attacks >= 2 && nonAttackPackets <= 1) {
                flagAndAlert("attacks=" + attacks + ", nonAttack=" + nonAttackPackets);
            } else {
                reward();
            }
            attacks = 0;
            nonAttackPackets = 0;
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity interact = new WrapperPlayClientInteractEntity(event);
            if (interact.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                attacks++;
                return;
            }
        }

        nonAttackPackets++;
    }
}
