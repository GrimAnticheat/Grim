package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Client;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction.Action;

@CheckData(name = "BadPacketsQ")
public class BadPacketsQ extends Check implements PacketCheck {
    public BadPacketsQ(final GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != Client.ENTITY_ACTION) return;

        final WrapperPlayClientEntityAction packet = new WrapperPlayClientEntityAction(event);

        if (!(packet.getJumpBoost() < 0 ||
                packet.getJumpBoost() > 100 ||
                packet.getEntityId() != player.entityID ||
                (packet.getAction() != Action.START_JUMPING_WITH_HORSE && packet.getJumpBoost() != 0))) return;
        
        if (flagAndAlert("boost=" + packet.getJumpBoost() + ", action=" + packet.getAction() + ", entity=" + packet.getEntityId()) && shouldModifyPackets()) {
            event.setCancelled(true);
            player.onPacketCancel();
        }
    }
}
