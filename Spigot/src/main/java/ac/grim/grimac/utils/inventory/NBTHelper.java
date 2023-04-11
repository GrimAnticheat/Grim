package ac.grim.grimac.utils.inventory;

import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.nbt.NBTNumber;

public class NBTHelper {
    public static int getBaseRepairCost(ItemStack itemStack) {
        if (itemStack.getNBT() == null)
            return 0;

        NBTNumber tag = itemStack.getNBT().getNumberTagOrNull("RepairCost");
        return tag == null ? 0 : tag.getAsInt();
    }
}
