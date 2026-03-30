package ac.grim.grimac.checks.impl.combat;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;

@CheckData(name = "SelfInteract", description = "Interacted with self")
public class SelfInteract extends Check implements PacketCheck {
    public SelfInteract(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY
                && player.cameraEntity.isSelf() // TODO: should check for camera entity id?
                && new WrapperPlayClientInteractEntity(event).getEntityId() == player.entityID
                && flagAndAlert() && shouldModifyPackets() // Instant ban
        ) {
            event.setCancelled(true);
            player.onPacketCancel();
        }
    }
}
