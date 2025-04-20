package ac.grim.grimac.platform.fabric.manager;

import ac.grim.grimac.platform.api.manager.ItemResetHandler;
import ac.grim.grimac.platform.api.player.PlatformPlayer;
import ac.grim.grimac.platform.fabric.player.AbstractFabricPlatformPlayer;
import org.jetbrains.annotations.Nullable;

public class FabricItemResetHandler implements ItemResetHandler {
    @Override
    public void resetItemUsage(@Nullable PlatformPlayer player) {
        if (player != null) {
            ((AbstractFabricPlatformPlayer) player).getFabricPlayer().clearActiveItem();
        }
    }
}
