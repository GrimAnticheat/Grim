package ac.grim.grimac.platform.api.player;

import com.github.retrooper.packetevents.protocol.item.ItemStack;

public interface PlatformInventory {
    ItemStack getItemInHand();

    ItemStack getItemInOffHand();

    ItemStack getStack(int bukkitSlot, int vanillaSlot);

    ItemStack getHelmet();

    ItemStack getChestplate();

    ItemStack getLeggings();

    ItemStack getBoots();

    ItemStack[] getContents();

    String getOpenInventoryKey();
}
