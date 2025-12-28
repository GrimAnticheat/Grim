package ac.grim.grimac.platform.fabric.mc1194.player;

import ac.grim.grimac.platform.fabric.mc1161.player.Fabric1161PlatformInventory;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;

public class Fabric1193PlatformInventory extends Fabric1161PlatformInventory {
    public Fabric1193PlatformInventory(ServerPlayer player) {
        super(player);
    }

    @Override
    protected ResourceLocation getScreenID(MenuType<?> type) {
        return BuiltInRegistries.MENU.getKey(type);
    }
}
