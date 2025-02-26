package ac.grim.grimac.platform.api.manager;

import ac.grim.grimac.platform.api.player.PlatformPlayer;
import ac.grim.grimac.platform.api.sender.Sender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface MessagePlaceHolderManager {
    @NonNull
    String replacePlaceholders(@Nullable Sender object, @NonNull String string);

    @NonNull
    String replacePlaceholders(@Nullable PlatformPlayer object, @NonNull String string);
}
