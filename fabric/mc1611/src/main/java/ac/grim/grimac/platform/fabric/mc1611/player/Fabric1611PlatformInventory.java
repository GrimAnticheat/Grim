package ac.grim.grimac.platform.fabric.mc1611.player;

import ac.grim.grimac.platform.fabric.player.FabricPlatformInventory;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class Fabric1611PlatformInventory extends FabricPlatformInventory {

    public Fabric1611PlatformInventory(ServerPlayerEntity player) {
        super(player);
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

        Identifier registryKey = Registry.SCREEN_HANDLER.getId(type);
        if (registryKey != null) {
            return registryKey.getPath();
        }

        return handler.getClass().getSimpleName(); // Default fallback
    }
}
