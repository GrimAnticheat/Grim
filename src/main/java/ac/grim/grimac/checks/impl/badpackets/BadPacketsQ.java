package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.AbstractPacketCheck;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.PacketHandlerRegistry;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction.Action;

@CheckData(name = "BadPacketsQ")
public class BadPacketsQ extends AbstractPacketCheck {
    public BadPacketsQ(final GrimPlayer player) {
        super(player);
    }

    @Override
    protected void registerReceiveHandlers(PacketHandlerRegistry<PacketReceiveEvent> registry) {
        registry.registerHandler(event -> {
            WrapperPlayClientEntityAction wrapper = new WrapperPlayClientEntityAction(event);
            // you are able to send negative jump boost, how and why!?
            if (Math.abs(wrapper.getJumpBoost()) > 100
                    || wrapper.getEntityId() != player.entityID
                    || wrapper.getAction() != Action.START_JUMPING_WITH_HORSE && wrapper.getJumpBoost() != 0) {
                if (flagAndAlert("boost=" + wrapper.getJumpBoost() + ", action=" + wrapper.getAction() + ", entity=" + wrapper.getEntityId()) && shouldModifyPackets()) {
                    event.setCancelled(true);
                    player.onPacketCancel();
                }
            }
        }, PacketType.Play.Client.ENTITY_ACTION);
    }
}
