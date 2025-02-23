package ac.grim.grimac.platform.api.manager;

import ac.grim.grimac.platform.api.player.PlatformPlayer;
import org.jetbrains.annotations.Nullable;

public interface ItemResetHandler {

    public void resetItemUsage(@Nullable PlatformPlayer player);
}
