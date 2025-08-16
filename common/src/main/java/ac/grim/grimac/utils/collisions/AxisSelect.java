package ac.grim.grimac.utils.collisions;

import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public enum AxisSelect {
    EAST {
        public SimpleCollisionBox modify(SimpleCollisionBox box) {
            box.maxX = 1;
            return box;
        }
    },
    WEST {
        public SimpleCollisionBox modify(SimpleCollisionBox box) {
            box.minX = 0;
            return box;
        }
    },
    NORTH {
        public SimpleCollisionBox modify(SimpleCollisionBox box) {
            box.minZ = 0;
            return box;
        }
    },
    SOUTH {
        public SimpleCollisionBox modify(SimpleCollisionBox box) {
            box.maxZ = 1;
            return box;
        }
    },
    UP {
        public SimpleCollisionBox modify(SimpleCollisionBox box) {
            box.minY = 0;
            return box;
        }
    },
    DOWN {
        public SimpleCollisionBox modify(SimpleCollisionBox box) {
            box.maxY = 1;
            return box;
        }
    };

    public abstract SimpleCollisionBox modify(SimpleCollisionBox box);

    @Contract(pure = true)
    public static AxisSelect byFace(@NotNull BlockFace face) {
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
