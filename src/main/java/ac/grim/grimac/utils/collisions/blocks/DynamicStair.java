package ac.grim.grimac.utils.collisions.blocks;

import ac.grim.grimac.utils.blockdata.WrappedBlockDataValue;
import ac.grim.grimac.utils.collisions.CollisionBox;
import ac.grim.grimac.utils.collisions.types.CollisionFactory;
import ac.grim.grimac.utils.collisions.types.SimpleCollisionBox;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import org.bukkit.block.data.BlockData;

public class DynamicStair implements CollisionFactory {

    public CollisionBox fetch(ClientVersion version, byte data, int x, int y, int z) {
        return new SimpleCollisionBox(0, 0, 0, 1, 1, 1);
    }

    public CollisionBox fetch(ClientVersion version, BlockData block, int x, int y, int z) {
        return new SimpleCollisionBox(0, 0, 0, 1, 1, 1);
    }

    @Override
    public CollisionBox fetch(ClientVersion version, WrappedBlockDataValue block, int x, int y, int z) {
        return null;
    }
}
