package ac.grim.grimac.platform.fabric.manager;

import ac.grim.grimac.platform.api.manager.ItemResetHandler;
import ac.grim.grimac.platform.api.player.PlatformPlayer;
import ac.grim.grimac.platform.fabric.inject.FabricServerPlayerHandle;
import ac.grim.grimac.platform.fabric.utils.convert.IFabricConversionUtil;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

@RequiredArgsConstructor
public class FabricItemResetHandler implements ItemResetHandler {
    private final IFabricConversionUtil conversionUtil;

    @Override
    public void resetItemUsage(@Nullable PlatformPlayer player) {
        if (player != null) {
            handle(player).stopUsingItem();
        }
    }

    @Override
    public @Nullable InteractionHand getItemUsageHand(@Nullable PlatformPlayer platformPlayer) {
        if (platformPlayer == null) {
            return null;
        }

        FabricServerPlayerHandle player = handle(platformPlayer);
        return player.isUsingItem() ? conversionUtil.fromFabricInteractionHand(player.usedItemHand()) : null;
    }

    @Override
    public boolean isUsingItem(@Nullable PlatformPlayer player) {
        return player != null && handle(player).isUsingItem();
    }

    private FabricServerPlayerHandle handle(PlatformPlayer player) {
        return (FabricServerPlayerHandle) player.getNative();
    }
}
