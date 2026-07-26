package ac.grim.grimac.platform.minestom.manager;

import ac.grim.grimac.platform.api.manager.ItemResetHandler;
import ac.grim.grimac.platform.api.player.PlatformPlayer;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import org.jetbrains.annotations.Nullable;

/**
 * Item-usage state (eating/blocking/drawing bow). Grim primarily derives this from packets;
 * this server-side handler is a safe no-op default.
 * <p>
 * TODO Phase 3: back these by Minestom's item-use state if a check needs the authoritative
 * server view rather than the packet-derived one.
 */
public final class MinestomItemResetHandler implements ItemResetHandler {

    @Override
    public void resetItemUsage(@Nullable PlatformPlayer player) {
        // no-op
    }

    @Override
    public @Nullable InteractionHand getItemUsageHand(@Nullable PlatformPlayer player) {
        return null;
    }

    @Override
    public boolean isUsingItem(@Nullable PlatformPlayer player) {
        return false;
    }
}
