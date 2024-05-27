package ac.grim.grimac.checks.impl.badpackets;

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

@CheckData(name = "BadPacketsM", experimental = true)
public class BadPacketsM extends Check implements PacketCheck {
    public BadPacketsM(final GrimPlayer player) {
        super(player);
    }

    // 1.7 players do not send INTERACT_AT, so we cannot check them
    private final boolean exempt = player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_7_10);
    private boolean sentInteractAt = false;

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) return;

        if (exempt) return;

        final WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);

        final PacketEntity entity = player.compensatedEntities.entityMap.get(wrapper.getEntityId());

        // For armor stands, vanilla clients send:
        //  - when renaming the armor stand or in spectator mode: INTERACT_AT + INTERACT
        //  - in all other cases: only INTERACT
        // Just exempt armor stands to be safe
        if (entity != null && entity.type == EntityTypes.ARMOR_STAND) return;

        switch (wrapper.getAction()) {
            // INTERACT_AT then INTERACT
            case INTERACT:
                if (!sentInteractAt) {
                    if (flagAndAlert("Missed Interact-At") && shouldModifyPackets()) {
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
                sentInteractAt = true;
                break;
        }
    }
}
