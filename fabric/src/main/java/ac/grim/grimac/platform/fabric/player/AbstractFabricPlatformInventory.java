package ac.grim.grimac.platform.fabric.player;

import ac.grim.grimac.platform.api.player.PlatformInventory;
import ac.grim.grimac.platform.fabric.GrimACFabricLoaderPlugin;
import ac.grim.grimac.platform.fabric.utils.convert.IFabricConversionUtil;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.server.network.ServerPlayerEntity;


public abstract class AbstractFabricPlatformInventory implements PlatformInventory {

    private static final IFabricConversionUtil fabricConversionUtil = GrimACFabricLoaderPlugin.LOADER.getFabricConversionUtil();
    protected ServerPlayerEntity fabricPlayer;
    protected PlayerInventory inventory;

    public AbstractFabricPlatformInventory(ServerPlayerEntity player) {
        this.fabricPlayer = player;
        this.inventory = player.inventory;
    }

    @Override
    public ItemStack getItemInHand() {
        return fabricConversionUtil.fromFabricItemStack(inventory.getMainHandStack());
    }

    @Override
    public ItemStack getItemInOffHand() {
        return fabricConversionUtil.fromFabricItemStack(inventory.getStack(40));
    }

    @Override
    public ItemStack getStack(int bukkitSlot, int vanillaSlot) {
        return fabricConversionUtil.fromFabricItemStack(inventory.getStack(bukkitSlot));
    }

    @Override
    public ItemStack getHelmet() {
        return fabricConversionUtil.fromFabricItemStack(inventory.getStack(39));
    }

    @Override
    public ItemStack getChestplate() {
        return fabricConversionUtil.fromFabricItemStack(inventory.getStack(38));
    }

    @Override
    public ItemStack getLeggings() {
        return fabricConversionUtil.fromFabricItemStack(inventory.getStack(37));
    }

    @Override
    public ItemStack getBoots() {
        return fabricConversionUtil.fromFabricItemStack(inventory.getStack(36));
    }

    @Override
    public ItemStack[] getContents() {
        ItemStack[] items = new ItemStack[inventory.size()];
        for (int i = 0; i < inventory.size(); i++) {
            items[i] = fabricConversionUtil.fromFabricItemStack(inventory.getStack(i));
        }
        return items;
    }
}
