package ac.grim.grimac.checks.impl.packetorder;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "PacketOrderC", experimental = true)
public class PacketOrderC extends Check implements PacketCheck {
    public PacketOrderC(final GrimPlayer player) {
        super(player);
    }

    // 1.7 players do not send INTERACT_AT, so we cannot check them
    private final boolean exempt = player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_7_10);
    private boolean sentInteractAt = false;
    private int requiredEntity;

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (exempt) {
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {

            final WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);

            final PacketEntity entity = player.compensatedEntities.entityMap.get(wrapper.getEntityId());

            // For armor stands, vanilla clients send:
            //  - when renaming the armor stand or in spectator mode: INTERACT_AT + INTERACT
            //  - in all other cases: only INTERACT
            // Just exempt armor stands to be safe
            if (entity != null && entity.getType() == EntityTypes.ARMOR_STAND) return;

            switch (wrapper.getAction()) {
                // INTERACT_AT then INTERACT
                case INTERACT:
                    if (!sentInteractAt) {
                        if (flagAndAlert("Missed Interact-At") && shouldModifyPackets()) {
                            event.setCancelled(true);
                            player.onPacketCancel();
                        }
                    } else if (wrapper.getEntityId() != requiredEntity) {
                        if (flagAndAlert("Wrong Entity, required=" + requiredEntity + ", entity=" + wrapper.getEntityId()) && shouldModifyPackets()) {
                            event.setCancelled(true);
                            player.onPacketCancel();
                        }
                    }
                    sentInteractAt = false;
                    break;
                case INTERACT_AT:
                    if (sentInteractAt) {
                        if (flagAndAlert("Missed Interact") && shouldModifyPackets()) {
                            event.setCancelled(true);
                            player.onPacketCancel();
                        }
                    }
                    requiredEntity = wrapper.getEntityId();
                    sentInteractAt = true;
                    break;
            }
        }

        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            if (sentInteractAt) {
                if (flagAndAlert("Missed Interact (Tick)") && shouldModifyPackets()) {
                    event.setCancelled(true);
                    player.onPacketCancel();
                }
            }
        }
    }
}
