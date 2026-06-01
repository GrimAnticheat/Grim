package ac.grim.grimac.platform.fabric.mc261;

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
        super(player);
    }

    @Override
    public String getOpenInventoryKey() {
        AbstractContainerMenu handler = ((ServerPlayer) fabricPlatformPlayer.getNative()).containerMenu;
        MenuType<?> type = getSafeType(handler);

        if (type == null) {
            if (handler instanceof InventoryMenu) {
                return "CRAFTING";
            } else if (((ServerPlayer) fabricPlatformPlayer.getNative()).isCreative()) {
                return "CREATIVE";
            }
        }

        if (type == MenuType.CRAFTING) {
            return "CRAFTING";
        } else if (type == MenuType.GENERIC_9x4) {
            return "PLAYER";
        } else if (type == MenuType.GENERIC_9x3) {
            return "CHEST";
        } else if (type == MenuType.GENERIC_3x3) {
            return "DISPENSER";
        } else {
            Identifier registryKey = BuiltInRegistries.MENU.getKey(type);
            if (registryKey != null) {
                return registryKey.getPath();
            }

            return handler.getClass().getSimpleName();
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
