package ac.grim.grimac.utils.collisions.blocks;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.CollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.CollisionFactory;
import ac.grim.grimac.utils.collisions.datatypes.HexCollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;

public class PistonBaseCollision implements CollisionFactory {

    @Override
    public CollisionBox fetch(GrimPlayer player, ClientVersion version, WrappedBlockState block, int x, int y, int z) {
        if (!block.isExtended()) return new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true);

        return switch (block.getFacing()) {
            case UP -> new HexCollisionBox(0, 0, 0, 16, 12, 16);
            case NORTH -> new HexCollisionBox(0, 0, 4, 16, 16, 16);
            case SOUTH -> new HexCollisionBox(0, 0, 0, 16, 16, 12);
            case WEST -> new HexCollisionBox(4, 0, 0, 16, 16, 16);
            case EAST -> new HexCollisionBox(0, 0, 0, 12, 16, 16);
            default -> new HexCollisionBox(0, 4, 0, 16, 16, 16);
        };
    }
}
