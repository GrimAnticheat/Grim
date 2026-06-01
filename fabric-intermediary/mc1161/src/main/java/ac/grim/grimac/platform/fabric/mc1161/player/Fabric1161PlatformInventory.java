package ac.grim.grimac.platform.fabric.mc1161.player;

import ac.grim.grimac.platform.fabric.player.AbstractFabricPlatformInventory;
import ac.grim.grimac.platform.fabric.player.AbstractFabricPlatformPlayer;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import org.jetbrains.annotations.Nullable;

public class Fabric1161PlatformInventory extends AbstractFabricPlatformInventory {
    public Fabric1161PlatformInventory(AbstractFabricPlatformPlayer player) {
        super(player);
    }

    @Override
    public String getOpenInventoryKey() {
        AbstractContainerMenu handler = ((ServerPlayer) fabricPlatformPlayer.getNative()).containerMenu;
        MenuType<?> type = getSafeType(handler);

        if (type == null) {
            if (handler instanceof InventoryMenu) {
                return "CRAFTING";
            } else if (this.isPlayerCreative()) {
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
            ResourceLocation registryKey = (ResourceLocation) this.getScreenID(type);
            if (registryKey != null) {
                return registryKey.getPath();
            }

            return handler.getClass().getSimpleName();
        }
    }

    protected Object getScreenID(MenuType<?> type) {
        return Registry.MENU.getKey(type);
    }

    protected boolean isPlayerCreative() {
        return ((ServerPlayer) fabricPlatformPlayer.getNative()).isCreative();
    }

    protected @Nullable MenuType<?> getSafeType(AbstractContainerMenu handler) {
        try {
            return handler.getType();
        } catch (UnsupportedOperationException e) {
            return null;
        }
    }
}
