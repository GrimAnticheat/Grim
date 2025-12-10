package ac.grim.grimac.platform.fabric.mc1161.player;

import ac.grim.grimac.platform.fabric.player.AbstractFabricPlatformInventory;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import org.jetbrains.annotations.Nullable;

public class Fabric1161PlatformInventory extends AbstractFabricPlatformInventory {
    public Fabric1161PlatformInventory(ServerPlayer player) {
        super(player);
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
        AbstractContainerMenu handler = fabricPlayer.containerMenu;
        MenuType<?> type = getSafeType(handler);

        // Handle null types (player crafting and creative)
        if (type == null) {
            // 4x4 CRAFTING -> CRAFTING
            if (handler instanceof InventoryMenu) {
                return "CRAFTING";
                // Not sure if creative mode check here is correct
            } else if (this.isPlayerCreative()) {
                return "CREATIVE";
            }
        }

        // should we handle crafters here also??
        // CRAFTING -> CRAFTING
        if (type == MenuType.CRAFTING) {
            return "CRAFTING";
            // PLAYER -> PLAYER
        } else if (type == MenuType.GENERIC_9x4) {
            return "PLAYER";
            // CHEST, ENDER_CHEST, or BARREL -> CHEST
        } else if (type == MenuType.GENERIC_9x3) {
            return "CHEST";
            // DISPENSER, DROPPER -> DISPENSER
        } else if (type == MenuType.GENERIC_3x3) {
            return "DISPENSER";
        } else {
            // Registry handles:
            // SHULKER_BOX -> SHULKER_BOX
            // CRAFTIING -> CRAFTING

            ResourceLocation registryKey = this.getScreenID(type);
            if (registryKey != null) {
                return registryKey.getPath();
            }

            return handler.getClass().getSimpleName(); // Default fallback
        }
    }

    protected ResourceLocation getScreenID(MenuType<?> type) {
        return Registry.MENU.getKey(type);
    }

    protected boolean isPlayerCreative() {
        return fabricPlayer.isCreative();
    }

    protected @Nullable MenuType<?> getSafeType(AbstractContainerMenu handler) {
        try {
            return handler.getType();
        } catch (UnsupportedOperationException e) {
            return null;
        }
    }
}
