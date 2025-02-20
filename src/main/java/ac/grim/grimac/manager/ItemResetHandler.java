package ac.grim.grimac.manager;

import ac.grim.grimac.player.PlatformPlayer;
import org.jetbrains.annotations.Nullable;

public interface ItemResetHandler {

    public void resetItemUsage(@Nullable PlatformPlayer player);
}
