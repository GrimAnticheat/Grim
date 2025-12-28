package ac.grim.grimac.platform.fabric.player;

import ac.grim.grimac.platform.api.player.PlatformInventory;
import ac.grim.grimac.platform.fabric.GrimACFabricLoaderPlugin;
import ac.grim.grimac.platform.fabric.utils.convert.IFabricConversionUtil;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;


public abstract class AbstractFabricPlatformInventory implements PlatformInventory {

    private static final IFabricConversionUtil fabricConversionUtil = GrimACFabricLoaderPlugin.LOADER.getFabricConversionUtil();
    protected ServerPlayer fabricPlayer;
    protected Inventory inventory;

    public AbstractFabricPlatformInventory(ServerPlayer player) {
        this.fabricPlayer = player;
        this.inventory = player.inventory;
    }

    @Override
    public ItemStack getItemInHand() {
        return fabricConversionUtil.fromFabricItemStack(inventory.getSelected());
    }

    @Override
    public ItemStack getItemInOffHand() {
        return fabricConversionUtil.fromFabricItemStack(inventory.getItem(40));
    }

    @Override
    public ItemStack getStack(int bukkitSlot, int vanillaSlot) {
        return fabricConversionUtil.fromFabricItemStack(inventory.getItem(bukkitSlot));
    }

    @Override
    public ItemStack getHelmet() {
        return fabricConversionUtil.fromFabricItemStack(inventory.getItem(39));
    }

    @Override
    public ItemStack getChestplate() {
        return fabricConversionUtil.fromFabricItemStack(inventory.getItem(38));
    }

    @Override
    public ItemStack getLeggings() {
        return fabricConversionUtil.fromFabricItemStack(inventory.getItem(37));
    }

    @Override
    public ItemStack getBoots() {
        return fabricConversionUtil.fromFabricItemStack(inventory.getItem(36));
    }

    @Override
    public ItemStack[] getContents() {
        ItemStack[] items = new ItemStack[inventory.getContainerSize()];
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            items[i] = fabricConversionUtil.fromFabricItemStack(inventory.getItem(i));
        }
        return items;
    }
}
