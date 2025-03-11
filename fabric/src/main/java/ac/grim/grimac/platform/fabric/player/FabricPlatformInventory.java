package ac.grim.grimac.platform.fabric.player;

import ac.grim.grimac.platform.api.player.PlatformInventory;
import ac.grim.grimac.platform.fabric.utils.convert.FabricConversionUtil;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.registry.Registries;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.checkerframework.checker.nullness.qual.Nullable;


public class FabricPlatformInventory implements PlatformInventory {

    protected final ServerPlayerEntity fabricPlayer;
    protected final PlayerInventory inventory;

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
    // I don't understand why we do this on Bukkit, so I'm replicating the behaviour without high-level understanding of purpose
    // This method is only used to check if the inventory matches one of the following
    //     private static final Set<String> SUPPORTED_INVENTORIES = new HashSet<>(
    //            Arrays.asList("CHEST", "DISPENSER", "DROPPER", "PLAYER", "ENDER_CHEST", "SHULKER_BOX", "BARREL", "CRAFTING", "CREATIVE")
    //    );
    // And is slated to be replaced by packet based behaviour, this should do for now
    @Override
    public String getOpenInventoryKey() {
        ScreenHandler handler = fabricPlayer.currentScreenHandler;
        ScreenHandlerType<?> type = getSafeType(handler);

        // Handle null types (player crafting and creative)
        if (type == null) {
            // 4x4 CRAFTING -> CRAFTING
            if (handler instanceof PlayerScreenHandler) {
                return "CRAFTING";
                // Not sure if creative mode check here is correct
            } else if (fabricPlayer.isInCreativeMode()) {
                return "CREATIVE";
            }
        }

        // should we handle crafters here also??
        // CRAFTING -> CRAFTING
        if (type == ScreenHandlerType.CRAFTING) {
            return "CRAFTING";
        // PLAYER -> PLAYER
        } else if (type == ScreenHandlerType.GENERIC_9X4) {
            return "PLAYER";
        // CHEST, ENDER_CHEST, or BARREL -> CHEST
        } else if (type == ScreenHandlerType.GENERIC_9X3) {
            return "CHEST";
        // DISPENSER, DROPPER -> DISPENSER
        } else if (type == ScreenHandlerType.GENERIC_3X3) {
            return "DISPENSER";
        } else {
            // Registry handles:
            // SHULKER_BOX -> SHULKER_BOX
            // CRAFTIING -> CRAFTING
            Identifier registryKey = Registries.SCREEN_HANDLER.getId(type);
            if (registryKey != null) {
                return registryKey.getPath();
            }

            return handler.getClass().getSimpleName(); // Default fallback
        }
    }

    private static @Nullable ScreenHandlerType<?> getSafeType(ScreenHandler handler) {
        try {
            return handler.getType();
        } catch (UnsupportedOperationException e) {
            return null;
        }
    }
}
