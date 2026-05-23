package ac.grim.grimac.platform.fabric.player;

import ac.grim.grimac.platform.api.player.PlatformInventory;
import ac.grim.grimac.platform.fabric.GrimACFabricLoaderPlugin;
import ac.grim.grimac.platform.fabric.utils.convert.IFabricConversionUtil;
import com.github.retrooper.packetevents.protocol.item.ItemStack;


public abstract class AbstractFabricPlatformInventory implements PlatformInventory {

    private static final IFabricConversionUtil fabricConversionUtil = GrimACFabricLoaderPlugin.LOADER.getFabricConversionUtil();
    protected final AbstractFabricPlatformPlayer fabricPlatformPlayer;

    public AbstractFabricPlatformInventory(AbstractFabricPlatformPlayer fabricPlatformPlayer) {
        this.fabricPlatformPlayer = fabricPlatformPlayer;
    }

    @Override
    public ItemStack getItemInHand() {
        return fabricConversionUtil.fromFabricItemStack(fabricPlatformPlayer.fabricPlayer.inventory.getSelected());
    }

    @Override
    public ItemStack getItemInOffHand() {
        return fabricConversionUtil.fromFabricItemStack(fabricPlatformPlayer.fabricPlayer.inventory.getItem(40));
    }

    @Override
    public ItemStack getStack(int bukkitSlot, int vanillaSlot) {
        return fabricConversionUtil.fromFabricItemStack(fabricPlatformPlayer.fabricPlayer.inventory.getItem(bukkitSlot));
    }

    @Override
    public ItemStack getHelmet() {
        return fabricConversionUtil.fromFabricItemStack(fabricPlatformPlayer.fabricPlayer.inventory.getItem(39));
    }

    @Override
    public ItemStack getChestplate() {
        return fabricConversionUtil.fromFabricItemStack(fabricPlatformPlayer.fabricPlayer.inventory.getItem(38));
    }

    @Override
    public ItemStack getLeggings() {
        return fabricConversionUtil.fromFabricItemStack(fabricPlatformPlayer.fabricPlayer.inventory.getItem(37));
    }

    @Override
    public ItemStack getBoots() {
        return fabricConversionUtil.fromFabricItemStack(fabricPlatformPlayer.fabricPlayer.inventory.getItem(36));
    }

    @Override
    public ItemStack[] getContents() {
        ItemStack[] items = new ItemStack[fabricPlatformPlayer.fabricPlayer.inventory.getContainerSize()];
        for (int i = 0; i < fabricPlatformPlayer.fabricPlayer.inventory.getContainerSize(); i++) {
            items[i] = fabricConversionUtil.fromFabricItemStack(fabricPlatformPlayer.fabricPlayer.inventory.getItem(i));
        }
        return items;
    }
}
