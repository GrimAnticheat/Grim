package ac.grim.grimac.platform.minestom.manager;

import ac.grim.grimac.platform.api.manager.MessagePlaceHolderManager;
import ac.grim.grimac.platform.api.player.PlatformPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * No third-party placeholder engine (PlaceholderAPI) exists on Minestom; returns the string
 * unchanged. TODO Phase 4: wire the monorepo's i18n/placeholder system if alert templates
 * need it.
 */
public final class MinestomMessagePlaceHolderManager implements MessagePlaceHolderManager {

    @Override
    public String replacePlaceholders(@Nullable PlatformPlayer player, @NotNull String string) {
        return string;
    }
}
