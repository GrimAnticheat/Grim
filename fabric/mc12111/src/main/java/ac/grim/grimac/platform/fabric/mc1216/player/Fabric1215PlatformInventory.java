package ac.grim.grimac.platform.fabric.mc1216.player;

import ac.grim.grimac.platform.fabric.mc1194.player.Fabric1193PlatformInventory;
import ac.grim.grimac.platform.fabric.player.AbstractFabricPlatformPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;

public class Fabric1215PlatformInventory extends Fabric1193PlatformInventory {
    public Fabric1215PlatformInventory(AbstractFabricPlatformPlayer player) {
        super(player);
    }

    @Override
    protected Object getScreenID(MenuType<?> type) {
        return BuiltInRegistries.MENU.getKey(type);
    }

    @Override
    protected boolean isPlayerCreative() {
        return fabricPlatformPlayer.getNative().isCreative();
    }
}
