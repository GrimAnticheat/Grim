package ac.grim.grimac.platform.fabric.player;

import ac.grim.grimac.platform.api.player.PlatformInventory;
import ac.grim.grimac.platform.fabric.utils.convert.FabricConversionUtil;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.registry.Registries;
import net.minecraft.screen.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;


public class FabricPlatformInventory implements PlatformInventory {

    private final ServerPlayerEntity fabricPlayer;
    private final PlayerInventory inventory;

    public FabricPlatformInventory(ServerPlayerEntity player) {
        this.fabricPlayer = player;
        this.inventory = player.getInventory();
    }

    @Override
    public ItemStack getItemInHand() {
        return FabricConversionUtil.fromFabricItemStack(inventory.getMainHandStack());
    }

    @Override
    public ItemStack getItemInOffHand() {
        return FabricConversionUtil.fromFabricItemStack(inventory.offHand.get(0));
    }

    @Override
    public ItemStack getStack(int bukkitSlot, int vanillaSlot) {
        return FabricConversionUtil.fromFabricItemStack(inventory.getStack(bukkitSlot));
    }

    @Override
    public ItemStack getHelmet() {
        return FabricConversionUtil.fromFabricItemStack(inventory.getArmorStack(3));
    }

    @Override
    public ItemStack getChestplate() {
        return FabricConversionUtil.fromFabricItemStack(inventory.getArmorStack(2));
    }

    @Override
    public ItemStack getLeggings() {
        return FabricConversionUtil.fromFabricItemStack(inventory.getArmorStack(1));
    }

    @Override
    public ItemStack getBoots() {
        return FabricConversionUtil.fromFabricItemStack(inventory.getArmorStack(0));
    }

    @Override
    public ItemStack[] getContents() {
        ItemStack[] items = new ItemStack[inventory.size()];
        for (int i = 0; i < inventory.size(); i++) {
            items[i] = FabricConversionUtil.fromFabricItemStack(inventory.getStack(i));
        }
        return items;
    }

    // TODO
    // This method is only used to check if the inventory matches one of the following
    //     private static final Set<String> SUPPORTED_INVENTORIES = new HashSet<>(
    //            Arrays.asList("CHEST", "DISPENSER", "DROPPER", "PLAYER", "ENDER_CHEST", "SHULKER_BOX", "BARREL", "CRAFTING", "CREATIVE")
    //    );
    // And is slated to be replaced by packet based behaviour, this should do for now
    @Override
    public String getOpenInventoryKey() {
        ScreenHandler handler = fabricPlayer.currentScreenHandler;
        ScreenHandlerType<?> type = handler.getType();

        // Handle null types (player crafting and creative)
        if (type == null) {
            if (handler instanceof PlayerScreenHandler) {
                return "CRAFTING";
            }
//            else if (fabricPlayer.isInCreativeMode() && ) {
//                return "CREATIVE";
//            }
        }

        // 4x4 CRAFTING -> CRAFTING
        // PLAYER -> PLAYER
        // CHEST, ENDER_CHEST, or BARREL -> CHEST
        // DISPENSER, DROPPER -> DISPENSER

        // Registry handles:
        // SHULKER_BOX -> SHULKER_BOX
        // CRAFTIING -> CRAFTING

        // Handle special mappings
        if (type == ScreenHandlerType.GENERIC_9X4) {
            return "PLAYER";
        } else if (type == ScreenHandlerType.GENERIC_9X3) {
            // Could be CHEST, ENDER_CHEST, or BARREL
            return "CHEST"; // They all use the same handler type
        } else if (type == ScreenHandlerType.GENERIC_3X3) {
            // Could be DISPENSER or DROPPER
            return "DISPENSER"; // They use the same handler type
        }

        Identifier registryKey = Registries.SCREEN_HANDLER.getId(type);
        if (registryKey != null) {
            return registryKey.getPath();
        }

        return handler.getClass().getSimpleName(); // Default fallback
    }
}
