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
            int inventorySize = player.getInventory().menu.getSlots().size();
            // -999 called when player click outside inventory, but if he does it with block, the slot id should be -1.
            boolean clickOutsideInventory = slotId == -999 || slotId == -1;
            boolean impossibleNegativeSlotId = !clickOutsideInventory && slotId < 0;
            // Furnace can produce 0 inventory slot size.
            boolean validInventorySize = inventorySize > 0;
            boolean higherThanMaxSlotSizeSlotId = slotId >= inventorySize;

            if (impossibleNegativeSlotId || validInventorySize && higherThanMaxSlotSizeSlotId) {
                if (flagAndAlert("slotId=" + slotId + " size=" + inventorySize) && shouldModifyPackets())
                    // Cancel packet with invalid slot id.
                    event.setCancelled(true);
            }
        }
    }
}
