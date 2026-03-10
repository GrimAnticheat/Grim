package ac.grim.grimac.checks.impl.combat;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;

@CheckData(name = "KillAuraH", description = "Multiple entity switches in one tick", decay = 0.02, experimental = true)
public class KillAuraH extends Check implements PostPredictionCheck {
    private int lastEntity = Integer.MIN_VALUE;
    private int switches;

    public KillAuraH(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity interact = new WrapperPlayClientInteractEntity(event);
            if (interact.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                int entity = interact.getEntityId();
                if (lastEntity != Integer.MIN_VALUE && entity != lastEntity && ++switches > 2) {
                    flagAndAlert("switches=" + switches);
                }
                lastEntity = entity;
            }
        }

        if (isTickPacket(event.getPacketType())) {
            switches = 0;
            lastEntity = Integer.MIN_VALUE;
        }
    }
}
