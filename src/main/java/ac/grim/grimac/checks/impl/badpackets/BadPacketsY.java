package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientHeldItemChange;

/**
 * Checks for out of bounds slot changes
 */
@CheckData(name = "BadPacketsY")
public class BadPacketsY extends Check implements PacketCheck {
    public BadPacketsY(final GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.HELD_ITEM_CHANGE) return;

        final int slot = new WrapperPlayClientHeldItemChange(event).getSlot();
        if (slot <= 8 && slot >= 0) return;

        // ban
        if (flagAndAlert("slot="+slot) && shouldModifyPackets()) {
            event.setCancelled(true);
            player.onPacketCancel();
        }
    }
}
