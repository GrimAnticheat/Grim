package ac.grim.grimac.platform.fabric.player;

import ac.grim.grimac.platform.api.player.PlatformInventory;
import ac.grim.grimac.platform.api.player.PlatformPlayer;
import ac.grim.grimac.platform.fabric.FabricPlatformServices;
import ac.grim.grimac.platform.fabric.inject.FabricServerPlayerHandle;
import ac.grim.grimac.platform.fabric.utils.FabricItemContextHook;
import com.github.retrooper.packetevents.protocol.item.ItemStack;

public abstract class AbstractFabricPlatformInventory implements PlatformInventory {

    protected final PlatformPlayer fabricPlatformPlayer;

    public AbstractFabricPlatformInventory(PlatformPlayer fabricPlatformPlayer) {
        this.fabricPlatformPlayer = fabricPlatformPlayer;
    }

    private FabricServerPlayerHandle handle() {
        return (FabricServerPlayerHandle) fabricPlatformPlayer.getNative();
    }

    /**
     * Converts a native item into a PacketEvents {@link ItemStack}. When Polymer's item codec needs it
     * (see {@link FabricItemContextHook#ACTIVE}), the encode runs with a {@code PacketContext} for the
     * inventory owner bound so Polymer can encode the item instead of crashing (GrimAnticheat/Grim#2701).
     * Otherwise it is the original direct read: the {@code ACTIVE} check folds away and no capturing
     * lambda is allocated, keeping this hot path (held item / armour reads during predictions) allocation-free.
     */
    private ItemStack convert(Object nativeItemStack) {
        if (!FabricItemContextHook.ACTIVE) {
            return FabricPlatformServices.conversionUtil().fromFabricItemStack(nativeItemStack);
        }
        return FabricItemContextHook.supply(
                fabricPlatformPlayer.getNative(),
                () -> FabricPlatformServices.conversionUtil().fromFabricItemStack(nativeItemStack));
    }

    @Override
    public ItemStack getItemInHand() {
        return convert(handle().heldItemStack());
    }

    @Override
    public ItemStack getItemInOffHand() {
        return convert(handle().inventoryItemAt(40));
    }

    @Override
    public ItemStack getStack(int bukkitSlot, int vanillaSlot) {
        return convert(handle().inventoryItemAt(bukkitSlot));
    }

    @Override
    public ItemStack getHelmet() {
        return convert(handle().inventoryItemAt(39));
    }

    @Override
    public ItemStack getChestplate() {
        return convert(handle().inventoryItemAt(38));
    }

    @Override
    public ItemStack getLeggings() {
        return convert(handle().inventoryItemAt(37));
    }

    @Override
    public ItemStack getBoots() {
        return convert(handle().inventoryItemAt(36));
    }

    @Override
    public ItemStack[] getContents() {
        if (!FabricItemContextHook.ACTIVE) {
            return readContents();
        }
        // Bind the context once for the whole inventory sweep rather than per slot.
        return FabricItemContextHook.supply(fabricPlatformPlayer.getNative(), this::readContents);
    }

    private ItemStack[] readContents() {
        FabricServerPlayerHandle handle = handle();
        ItemStack[] items = new ItemStack[handle.inventorySlotCount()];
        for (int i = 0; i < items.length; i++) {
            items[i] = FabricPlatformServices.conversionUtil().fromFabricItemStack(handle.inventoryItemAt(i));
        }
        return items;
    }
}
