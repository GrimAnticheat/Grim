package ac.grim.grimac.platform.fabric.player;

import ac.grim.grimac.platform.api.player.PlatformInventory;
import ac.grim.grimac.platform.api.player.PlatformPlayer;
import ac.grim.grimac.platform.fabric.FabricPlatformServices;
import ac.grim.grimac.platform.fabric.inject.FabricServerPlayerHandle;
import com.github.retrooper.packetevents.protocol.item.ItemStack;

public abstract class AbstractFabricPlatformInventory implements PlatformInventory {

    protected final PlatformPlayer fabricPlatformPlayer;

    public AbstractFabricPlatformInventory(PlatformPlayer fabricPlatformPlayer) {
        this.fabricPlatformPlayer = fabricPlatformPlayer;
    }

    private FabricServerPlayerHandle handle() {
        return (FabricServerPlayerHandle) fabricPlatformPlayer.getNative();
    }

    @Override
    public ItemStack getItemInHand() {
        return FabricPlatformServices.conversionUtil().fromFabricItemStack(handle().heldItemStack());
    }

    @Override
    public ItemStack getItemInOffHand() {
        return FabricPlatformServices.conversionUtil().fromFabricItemStack(handle().inventoryItemAt(40));
    }

    @Override
    public ItemStack getStack(int bukkitSlot, int vanillaSlot) {
        return FabricPlatformServices.conversionUtil().fromFabricItemStack(handle().inventoryItemAt(bukkitSlot));
    }

    @Override
    public ItemStack getHelmet() {
        return FabricPlatformServices.conversionUtil().fromFabricItemStack(handle().inventoryItemAt(39));
    }

    @Override
    public ItemStack getChestplate() {
        return FabricPlatformServices.conversionUtil().fromFabricItemStack(handle().inventoryItemAt(38));
    }

    @Override
    public ItemStack getLeggings() {
        return FabricPlatformServices.conversionUtil().fromFabricItemStack(handle().inventoryItemAt(37));
    }

    @Override
    public ItemStack getBoots() {
        return FabricPlatformServices.conversionUtil().fromFabricItemStack(handle().inventoryItemAt(36));
    }

    @Override
    public ItemStack[] getContents() {
        FabricServerPlayerHandle handle = handle();
        ItemStack[] items = new ItemStack[handle.inventorySlotCount()];
        for (int i = 0; i < items.length; i++) {
            items[i] = FabricPlatformServices.conversionUtil().fromFabricItemStack(handle.inventoryItemAt(i));
        }
        return items;
    }
}
