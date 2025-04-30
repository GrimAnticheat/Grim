package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import org.jetbrains.annotations.NotNull;

@CheckData(name = "BadPacketsZ", description = "Invalid slot ids out of the inventory size.")
public class BadPacketsZ extends Check implements PacketCheck {

    public BadPacketsZ(@NotNull GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {
            WrapperPlayClientClickWindow wrapper = new WrapperPlayClientClickWindow(event);
            int slotId = wrapper.getSlot();
            boolean clickOutsideInventory = slotId == -999;
            boolean impossibleNegativeSlotId = !clickOutsideInventory && slotId < 0;
            boolean higherThanMaxSlotSizeSlotId = slotId >= player.getInventory().getPacketSendingInventorySize();

            if (impossibleNegativeSlotId || higherThanMaxSlotSizeSlotId) {
                if (flagAndAlert("slotId=" + slotId) && shouldModifyPackets())
                    // Cancel packet with invalid slot id.
                    event.setCancelled(true);
            }
        }
    }
}
