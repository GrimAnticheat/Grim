package ac.grim.grimac.utils.anticheat.update;

import io.github.retrooper.packetevents.utils.player.Direction;
import io.github.retrooper.packetevents.utils.vector.Vector3i;

public class BlockPlace {
    Vector3i blockPosition;
    Direction face;
    boolean isCancelled = false;

    public BlockPlace(Vector3i blockPosition, Direction face) {
        this.blockPosition = blockPosition;
        this.face = face;
    }

    public Vector3i getPlacedAgainstBlockLocation() {
        return blockPosition;
    }

    public Direction getFace() {
        return face;
    }

    public boolean isCancelled() {
        return isCancelled;
    }

    public Vector3i getPlacedBlockPos() {
        int x = blockPosition.getX() + getNormalBlockFace().getX();
        int y = blockPosition.getY() + getNormalBlockFace().getY();
        int z = blockPosition.getZ() + getNormalBlockFace().getZ();
        return new Vector3i(x, y, z);
    }

    public Vector3i getNormalBlockFace() {
        switch (face) {
            default:
            case UP:
                return new Vector3i(0, 1, 0);
            case DOWN:
                return new Vector3i(0, -1, 0);
            case SOUTH:
                return new Vector3i(0, 0, 1);
            case NORTH:
                return new Vector3i(0, 0, -1);
            case WEST:
                return new Vector3i(-1, 0, 0);
            case EAST:
                return new Vector3i(1, 0, 0);
        }
    }

    public void resync() {
        isCancelled = true;
    }
}
