package ac.grim.grimac.platform.fabric.mc261;

import ac.grim.grimac.platform.fabric.GrimACFabricLoaderPlugin;
import ac.grim.grimac.platform.fabric.player.AbstractFabricPlatformInventory;
import ac.grim.grimac.platform.fabric.player.AbstractFabricPlatformPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import org.jetbrains.annotations.Nullable;

public class Fabric261PlatformInventory extends AbstractFabricPlatformInventory {
    public Fabric261PlatformInventory(AbstractFabricPlatformPlayer player) {
        super(player, GrimACFabricLoaderPlugin.LOADER.getFabricConversionUtil());
    }

    // Replicates intermediary's Fabric1161PlatformInventory.getOpenInventoryKey logic on
    // the 26.X namespace. 26.X drift vs the intermediary copy: ResourceLocation is now
    // net.minecraft.resources.Identifier (Registry.getKey returns Identifier), and the
    // registry lookup uses BuiltInRegistries.MENU like the 1.21.5+ override does. The
    // mapped strings (CRAFTING / PLAYER / CHEST / DISPENSER / registry path) match.
    @Override
    public String getOpenInventoryKey() {
        AbstractContainerMenu handler = ((ServerPlayer) fabricPlatformPlayer.getNative()).containerMenu;
        MenuType<?> type = getSafeType(handler);

        // Handle null types (player crafting and creative)
        if (type == null) {
            // 4x4 CRAFTING -> CRAFTING
            if (handler instanceof InventoryMenu) {
                return "CRAFTING";
                // Not sure if creative mode check here is correct
            } else if (((ServerPlayer) fabricPlatformPlayer.getNative()).isCreative()) {
                return "CREATIVE";
            }
        }

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
            // Registry handles e.g. SHULKER_BOX -> SHULKER_BOX
            Identifier registryKey = BuiltInRegistries.MENU.getKey(type);
            if (registryKey != null) {
                return registryKey.getPath();
            }

            return handler.getClass().getSimpleName(); // Default fallback
        }
    }

    protected @Nullable MenuType<?> getSafeType(AbstractContainerMenu handler) {
        try {
            return handler.getType();
        } catch (UnsupportedOperationException e) {
            return null;
        }
    }
}
