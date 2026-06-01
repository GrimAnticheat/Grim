package ac.grim.grimac.platform.fabric.utils.convert;

import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

public interface IFabricConversionUtil {

    ItemStack fromFabricItemStack(Object fabricItemStack);

    Object toNativeText(Component component);

    GameMode fromNativeGameMode(Object gameMode);

    Object toNativeGameMode(GameMode gameMode);

    @Nullable InteractionHand fromFabricInteractionHand(@Nullable Object hand);
}
