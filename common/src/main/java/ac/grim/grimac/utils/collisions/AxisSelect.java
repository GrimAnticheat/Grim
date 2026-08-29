package ac.grim.grimac.utils.collisions;

import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public enum AxisSelect {
    EAST {
        @Contract("_ -> param1")
        public @NotNull SimpleCollisionBox modify(@NotNull SimpleCollisionBox box) {
            box.maxX = 1;
            return box;
        }
    },
    WEST {
        @Contract("_ -> param1")
        public @NotNull SimpleCollisionBox modify(@NotNull SimpleCollisionBox box) {
            box.minX = 0;
            return box;
        }
    },
    NORTH {
        @Contract("_ -> param1")
        public @NotNull SimpleCollisionBox modify(@NotNull SimpleCollisionBox box) {
            box.minZ = 0;
            return box;
        }
    },
    SOUTH {
        @Contract("_ -> param1")
        public @NotNull SimpleCollisionBox modify(@NotNull SimpleCollisionBox box) {
            box.maxZ = 1;
            return box;
        }
    },
    UP {
        @Contract("_ -> param1")
        public @NotNull SimpleCollisionBox modify(@NotNull SimpleCollisionBox box) {
            box.minY = 0;
            return box;
        }
    },
    DOWN {
        @Contract("_ -> param1")
        public @NotNull SimpleCollisionBox modify(@NotNull SimpleCollisionBox box) {
            box.maxY = 1;
            return box;
        }
    };

    @Contract("_ -> param1")
    public abstract @NotNull SimpleCollisionBox modify(@NotNull SimpleCollisionBox box);

    @Contract(pure = true)
    public static @NotNull AxisSelect byFace(@NotNull BlockFace face) {
        return switch (face) {
            case EAST -> EAST;
            case WEST -> WEST;
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case UP -> UP;
            default -> DOWN;
        };
    }
}
