package ac.grim.grimac.platform.fabric.mc1216.player;

import ac.grim.grimac.platform.fabric.mc1194.player.Fabric1193PlatformInventory;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;

public class Fabric1215PlatformInventory extends Fabric1193PlatformInventory {
    public Fabric1215PlatformInventory(ServerPlayer player) {
        super(player);
    }

    @Override
    protected ResourceLocation getScreenID(MenuType<?> type) {
        return BuiltInRegistries.MENU.getKey(type);
    }

    @Override
    protected boolean isPlayerCreative() {
        return fabricPlayer.isCreative();
    }
}
