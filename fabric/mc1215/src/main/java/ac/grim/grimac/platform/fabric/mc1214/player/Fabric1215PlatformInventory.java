package ac.grim.grimac.platform.fabric.mc1214.player;

import ac.grim.grimac.platform.fabric.mc1194.player.Fabric1193PlatformInventory;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class Fabric1215PlatformInventory extends Fabric1193PlatformInventory {
    public Fabric1215PlatformInventory(ServerPlayerEntity player) {
        super(player);
    }

    @Override
    protected Identifier getScreenID(ScreenHandlerType<?> type) {
        return Registries.SCREEN_HANDLER.getId(type);
    }

    @Override
    protected boolean isPlayerCreative() {
        return fabricPlayer.isCreative();
    }
}
