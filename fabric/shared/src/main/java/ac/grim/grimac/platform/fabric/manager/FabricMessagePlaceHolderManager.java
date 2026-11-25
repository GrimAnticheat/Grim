package ac.grim.grimac.platform.fabric.manager;

import ac.grim.grimac.platform.api.manager.MessagePlaceHolderManager;
import ac.grim.grimac.platform.api.player.PlatformPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FabricMessagePlaceHolderManager implements MessagePlaceHolderManager {

    // PlaceHolderAPI doesn't exist on Fabric and no chosen replacement for the platform yet
    @Override
    public @NotNull String replacePlaceholders(@Nullable PlatformPlayer player, @NotNull String string) {
        return string;
    }
}
