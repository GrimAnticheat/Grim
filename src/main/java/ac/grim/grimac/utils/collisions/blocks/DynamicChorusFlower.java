package ac.grim.grimac.utils.collisions.blocks;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.blockdata.types.WrappedBlockDataValue;
import ac.grim.grimac.utils.collisions.datatypes.CollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.CollisionFactory;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import org.bukkit.block.data.BlockData;

public class DynamicChorusFlower implements CollisionFactory {
    public CollisionBox fetch(ClientVersion version, byte data, int x, int y, int z) {
        return new SimpleCollisionBox(0, 0, 0, 1, 1, 1);
    }

    public CollisionBox fetch(ClientVersion version, BlockData block, int x, int y, int z) {
        return new SimpleCollisionBox(0, 0, 0, 1, 1, 1);
    }

    @Override
    public CollisionBox fetch(GrimPlayer player, ClientVersion version, WrappedBlockDataValue block, int x, int y, int z) {
        return null;
    }
}
