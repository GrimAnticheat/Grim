package ac.grim.grimac.platform.fabric.utils.convert;

import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import net.minecraft.core.Direction;
import net.minecraft.world.level.GameType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

public abstract class FabricIntermediaryConversionUtil {

    public static GameType toFabricGameMode(GameMode gameMode) {
        return switch (gameMode) {
            case CREATIVE -> GameType.CREATIVE;
            case SURVIVAL -> GameType.SURVIVAL;
            case ADVENTURE -> GameType.ADVENTURE;
            case SPECTATOR -> GameType.SPECTATOR;
        };
    }

    public static GameMode fromFabricGameMode(GameType fabricGameMode) {
        return switch (fabricGameMode) {
            case CREATIVE -> GameMode.CREATIVE;
            case SURVIVAL -> GameMode.SURVIVAL;
            case ADVENTURE -> GameMode.ADVENTURE;
            case SPECTATOR -> GameMode.SPECTATOR;
            default -> throw new IllegalArgumentException("Unknown Fabric GameMode: " + fabricGameMode);
        };
    }

    @Contract(value = "null -> null; !null -> !null", pure = true)
    public static @Nullable InteractionHand fromFabricInteractionHand(@Nullable net.minecraft.world.InteractionHand hand) {
        return hand == null ? null : switch (hand) {
            case OFF_HAND -> InteractionHand.OFF_HAND;
            case MAIN_HAND -> InteractionHand.MAIN_HAND;
        };
    }

    public static BlockFace fromDirection(Direction direction) {
        return switch (direction) {
            case NORTH -> BlockFace.NORTH;
            case SOUTH -> BlockFace.SOUTH;
            case WEST  -> BlockFace.WEST;
            case EAST  -> BlockFace.EAST;
            case UP    -> BlockFace.UP;
            case DOWN  -> BlockFace.DOWN;
        };
    }
}
