package ac.grim.grimac.platform.fabric.manager;

import ac.grim.grimac.platform.api.manager.ItemResetHandler;
import ac.grim.grimac.platform.api.player.PlatformPlayer;
import ac.grim.grimac.platform.fabric.player.AbstractFabricPlatformPlayer;
import ac.grim.grimac.platform.fabric.utils.convert.FabricConversionUtil;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

public class FabricItemResetHandler implements ItemResetHandler {
    @Override
    public void resetItemUsage(@Nullable PlatformPlayer player) {
        if (player != null) {
            ((AbstractFabricPlatformPlayer) player).getFabricPlayer().clearActiveItem();
        }
    }

    @Override
    public @Nullable InteractionHand getItemUsageHand(@Nullable PlatformPlayer platformPlayer) {
        if (platformPlayer == null) {
            return null;
        }

        ServerPlayerEntity player = ((AbstractFabricPlatformPlayer) platformPlayer).getFabricPlayer();
        return player.isUsingItem() ? FabricConversionUtil.fromFabricHand(player.getActiveHand()) : null;
    }
}
