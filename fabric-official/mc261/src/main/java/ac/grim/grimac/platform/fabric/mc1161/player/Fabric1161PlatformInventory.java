package ac.grim.grimac.platform.fabric.mc1161.player;

import ac.grim.grimac.platform.fabric.player.AbstractFabricPlatformInventory;
import ac.grim.grimac.platform.fabric.player.AbstractFabricPlatformPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public class Fabric1161PlatformInventory extends AbstractFabricPlatformInventory {
    public Fabric1161PlatformInventory(AbstractFabricPlatformPlayer player) {
        super(player);
    }

    // This key is used only for inventory support gating against the canonical
    // uppercase constants in SUPPORTED_INVENTORIES (e.g. CHEST, SHULKER_BOX).
    // We therefore normalize all fallback paths to the same uppercase key style.
    @Override
    public String getOpenInventoryKey() {
        AbstractContainerMenu handler = fabricPlatformPlayer.getNative().containerMenu;
        MenuType<?> type = getSafeType(handler);

        // Handle null types (player crafting and creative)
        if (type == null) {
            // 4x4 CRAFTING -> CRAFTING
            if (handler instanceof InventoryMenu) {
                return "CRAFTING";
            } else if (this.isPlayerCreative()) {
                return "CREATIVE";
            }
            return normalizeFallbackKey(handler);
        }

        // CRAFTING -> CRAFTING
        if (type == MenuType.CRAFTING) {
            return "CRAFTING";
            // Generic chest menus (all row counts) -> CHEST
        } else if (type == MenuType.GENERIC_9x1
                || type == MenuType.GENERIC_9x2
                || type == MenuType.GENERIC_9x3
                || type == MenuType.GENERIC_9x4
                || type == MenuType.GENERIC_9x5
                || type == MenuType.GENERIC_9x6) {
            return "CHEST";
            // DISPENSER, DROPPER -> DISPENSER
        } else if (type == MenuType.GENERIC_3x3) {
            return "DISPENSER";
        } else {
            // Registry handles:
            // SHULKER_BOX -> SHULKER_BOX
            // CRAFTING -> CRAFTING

            Identifier registryKey = (Identifier) this.getScreenID(type);
            if (registryKey != null) {
                return registryKey.getPath().toUpperCase(Locale.ROOT).replace('.', '_');
            }

            return normalizeFallbackKey(handler); // Default fallback
        }
    }

    protected String normalizeFallbackKey(AbstractContainerMenu handler) {
        String simpleName = handler.getClass().getSimpleName().replace('$', '_');
        return simpleName.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toUpperCase(Locale.ROOT);
    }

    // returns Identifier in > 1.21.11, and ResourceLocation in 1.21.10-, which both map to class_2960
    // Compiler doesn't know that though and throws a fit, thus we make it return Object and cast to class_2960
    protected Object getScreenID(MenuType<?> type) {
        return BuiltInRegistries.MENU.getKey(type);
    }

    protected boolean isPlayerCreative() {
        return fabricPlatformPlayer.getNative().isCreative();
    }

    protected @Nullable MenuType<?> getSafeType(AbstractContainerMenu handler) {
        try {
            return handler.getType();
        } catch (UnsupportedOperationException e) {
            return null;
        }
    }
}
