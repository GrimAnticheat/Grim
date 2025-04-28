package ac.grim.grimac.platform.fabric.mc1194.player;

import ac.grim.grimac.platform.fabric.mc1161.player.Fabric1161PlatformInventory;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class Fabric1193PlatformInventory extends Fabric1161PlatformInventory {
    public Fabric1193PlatformInventory(ServerPlayerEntity player) {
        super(player);
    }

    @Override
    protected Identifier getScreenID(ScreenHandlerType<?> type) {
        return Registries.SCREEN_HANDLER.getId(type);
    }
}
