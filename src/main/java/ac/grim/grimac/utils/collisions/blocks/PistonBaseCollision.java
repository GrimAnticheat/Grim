package ac.grim.grimac.utils.collisions.blocks;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.blockdata.types.WrappedBlockDataValue;
import ac.grim.grimac.utils.collisions.datatypes.CollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.CollisionFactory;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import org.bukkit.block.data.BlockData;

public class PistonBaseCollision implements CollisionFactory {
    public CollisionBox fetch(ClientVersion version, byte data, int x, int y, int z) {
        //byte data = block.getState().getData().getData();

        if ((data & 8) != 0) {
            switch (data & 7) {
                case 0:
                    return new SimpleCollisionBox(0.0F, 0.25F, 0.0F, 1.0F, 1.0F, 1.0F);
                case 1:
                    return new SimpleCollisionBox(0.0F, 0.0F, 0.0F, 1.0F, 0.75F, 1.0F);
                case 2:
                    return new SimpleCollisionBox(0.0F, 0.0F, 0.25F, 1.0F, 1.0F, 1.0F);
                case 3:
                    return new SimpleCollisionBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 0.75F);
                case 4:
                    return new SimpleCollisionBox(0.25F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
                case 5:
                    return new SimpleCollisionBox(0.0F, 0.0F, 0.0F, 0.75F, 1.0F, 1.0F);
            }
        } else {
            return new SimpleCollisionBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
        }
        return null;
    }

    public CollisionBox fetch(ClientVersion version, BlockData block, int x, int y, int z) {
        return fetch(version, (byte) 0, x, y, z);
    }

    @Override
    public CollisionBox fetch(GrimPlayer player, ClientVersion version, WrappedBlockDataValue block, int x, int y, int z) {
        return null;
    }
}
