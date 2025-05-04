package ac.grim.grimac.platform.fabric.mc1161.player;

import ac.grim.grimac.platform.fabric.player.AbstractFabricPlatformInventory;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Fabric1161PlatformInventory extends AbstractFabricPlatformInventory {
    public Fabric1161PlatformInventory(ServerPlayerEntity player) {
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
        ScreenHandler handler = fabricPlayer.currentScreenHandler;
        ScreenHandlerType<?> type = getSafeType(handler);

        // Handle null types (player crafting and creative)
        if (type == null) {
            // 4x4 CRAFTING -> CRAFTING
            if (handler instanceof PlayerScreenHandler) {
                return "CRAFTING";
                // Not sure if creative mode check here is correct
            } else if (this.isPlayerCreative()) {
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

            Identifier registryKey = this.getScreenID(type);
            if (registryKey != null) {
                return registryKey.getPath();
            }

            return handler.getClass().getSimpleName(); // Default fallback
        }
    }

    protected Identifier getScreenID(ScreenHandlerType<?> type) {
        return Registry.SCREEN_HANDLER.getId(type);
    }

    protected boolean isPlayerCreative() {
        return fabricPlayer.isCreative();
    }

    protected @Nullable ScreenHandlerType<?> getSafeType(ScreenHandler handler) {
        try {
            return handler.getType();
        } catch (UnsupportedOperationException e) {
            return null;
        }
    }
}
