package ac.grim.grimac.platform.fabric.player;

import ac.grim.grimac.platform.api.player.PlatformInventory;
import ac.grim.grimac.platform.api.player.PlatformPlayer;
import ac.grim.grimac.platform.fabric.inject.FabricServerPlayerHandle;
import ac.grim.grimac.platform.fabric.utils.convert.IFabricConversionUtil;
import com.github.retrooper.packetevents.protocol.item.ItemStack;

// Single shared copy (lives in NMS-free fabric-common): every item read goes through the
// Loom-injected FabricServerPlayerHandle, so the one per-version divergence #14
// (Inventory.getSelected vs getSelectedItem) and the version-stable getItem/getContainerSize
// calls are written ONCE here instead of duplicated per aggregator. The player is held as the
// NMS-free PlatformPlayer; getNative() is read fresh through handle() each call so a
// respawn/dimension rebind is observed. Per-version subclasses (Fabric<ver>PlatformInventory)
// still extend this for getOpenInventoryKey, whose containerMenu/registry/isCreative usage is
// inherently per-mapping; they reach the raw native via fabricPlatformPlayer.getNative().
// The IFabricConversionUtil is passed in (the per-version subclass fetches it from its loader)
// because GrimACFabricLoaderPlugin is a per-version class this module cannot reference.
public abstract class AbstractFabricPlatformInventory implements PlatformInventory {

    private final IFabricConversionUtil fabricConversionUtil;
    protected final PlatformPlayer fabricPlatformPlayer;

    public AbstractFabricPlatformInventory(PlatformPlayer fabricPlatformPlayer, IFabricConversionUtil fabricConversionUtil) {
        this.fabricPlatformPlayer = fabricPlatformPlayer;
        this.fabricConversionUtil = fabricConversionUtil;
    }

    /** Current native player as the Loom-injected handle (fresh each call, tracks respawn rebinds). */
    private FabricServerPlayerHandle handle() {
        return (FabricServerPlayerHandle) fabricPlatformPlayer.getNative();
    }

    @Override
    public ItemStack getItemInHand() {
        // #14: getSelected() (<=1.21.x) / getSelectedItem() (26.x) unified by the bridge.
        return fabricConversionUtil.fromFabricItemStack(handle().heldItemStack());
    }

    @Override
    public ItemStack getItemInOffHand() {
        return fabricConversionUtil.fromFabricItemStack(handle().inventoryItemAt(40));
    }

    @Override
    public ItemStack getStack(int bukkitSlot, int vanillaSlot) {
        return fabricConversionUtil.fromFabricItemStack(handle().inventoryItemAt(bukkitSlot));
    }

    @Override
    public ItemStack getHelmet() {
        return fabricConversionUtil.fromFabricItemStack(handle().inventoryItemAt(39));
    }

    @Override
    public ItemStack getChestplate() {
        return fabricConversionUtil.fromFabricItemStack(handle().inventoryItemAt(38));
    }

    @Override
    public ItemStack getLeggings() {
        return fabricConversionUtil.fromFabricItemStack(handle().inventoryItemAt(37));
    }

    @Override
    public ItemStack getBoots() {
        return fabricConversionUtil.fromFabricItemStack(handle().inventoryItemAt(36));
    }

    @Override
    public ItemStack[] getContents() {
        FabricServerPlayerHandle handle = handle();
        ItemStack[] items = new ItemStack[handle.inventorySlotCount()];
        for (int i = 0; i < items.length; i++) {
            items[i] = fabricConversionUtil.fromFabricItemStack(handle.inventoryItemAt(i));
        }
        return items;
    }
}
