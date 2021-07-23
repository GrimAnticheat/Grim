package ac.grim.grimac.utils.collisions.blocks;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.blockdata.types.WrappedBlockDataValue;
import ac.grim.grimac.utils.blockdata.types.WrappedPistonBase;
import ac.grim.grimac.utils.collisions.datatypes.CollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.CollisionFactory;
import ac.grim.grimac.utils.collisions.datatypes.HexCollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import io.github.retrooper.packetevents.utils.player.ClientVersion;

public class PistonBaseCollision implements CollisionFactory {

    @Override
    public CollisionBox fetch(GrimPlayer player, ClientVersion version, WrappedBlockDataValue block, int x, int y, int z) {
        WrappedPistonBase base = (WrappedPistonBase) block;

        if (!base.isPowered()) return new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true);

        switch (base.getDirection()) {
            default:
            case DOWN:
                return new HexCollisionBox(0, 4, 0, 16, 16, 16);
            case UP:
                return new HexCollisionBox(0, 0, 0, 16, 12, 16);
            case NORTH:
                return new HexCollisionBox(0, 0, 4, 16, 16, 16);
            case SOUTH:
                return new HexCollisionBox(0, 0, 0, 16, 16, 12);
            case WEST:
                return new HexCollisionBox(4, 0, 0, 16, 16, 16);
            case EAST:
                return new HexCollisionBox(0, 0, 0, 12, 16, 16);
        }
    }
}
