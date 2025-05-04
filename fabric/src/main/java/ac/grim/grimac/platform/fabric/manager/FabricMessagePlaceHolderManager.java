package ac.grim.grimac.platform.fabric.manager;

import ac.grim.grimac.platform.api.manager.MessagePlaceHolderManager;
import ac.grim.grimac.platform.api.player.PlatformPlayer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class FabricMessagePlaceHolderManager implements MessagePlaceHolderManager {

    // PlaceHolderAPI doesn't exist on Fabric and no chosen replacement for the platform yet
    @Override
    public @NonNull String replacePlaceholders(@Nullable PlatformPlayer player, @NonNull String string) {
        return string;
    }
}
