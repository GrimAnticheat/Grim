package ac.grim.grimac.checks.impl.combat.killaura;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

/**
 * You can't attack more than one unique entity within a single client tick. This check simply
 * enforces that rule by checking whether or not the player attacked an entity with different
 * IDs more than once within a single client tick.
 */

@CheckData(name = "KillAuraA")
public class KillAuraA extends PacketCheck {
    private int ticks, lastEntityId;

    public KillAuraA(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);

            if (packet.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) return;

            if (packet.getEntityId() != lastEntityId) {
                if (++ticks > 1) {
                    flagAndAlert();
                }
            }

            lastEntityId = packet.getEntityId();
        } else if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            ticks = 0;
        }
    }
}
