package ac.grim.grimac.utils.collisions.blocks;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.blockdata.WrappedBlockDataValue;
import ac.grim.grimac.utils.collisions.CollisionBox;
import ac.grim.grimac.utils.collisions.types.CollisionFactory;
import ac.grim.grimac.utils.collisions.types.SimpleCollisionBox;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import org.bukkit.block.data.BlockData;

public class TrapDoorHandler implements CollisionFactory {
    public CollisionBox fetch(ClientVersion version, byte data, int x, int y, int z) {
        //byte data = block.getState().getData().getData();
        double var2 = 0.1875;

        if ((data & 4) != 0) {
            if ((data & 3) == 0) {
                return new SimpleCollisionBox(0.0, 0.0, 1.0 - var2, 1.0, 1.0, 1.0);
            }

            if ((data & 3) == 1) {
                return new SimpleCollisionBox(0.0, 0.0, 0.0, 1.0, 1.0, var2);
            }

            if ((data & 3) == 2) {
                return new SimpleCollisionBox(1.0 - var2, 0.0, 0.0, 1.0, 1.0, 1.0);
            }

            if ((data & 3) == 3) {
                return new SimpleCollisionBox(0.0, 0.0, 0.0, var2, 1.0, 1.0);
            }
        } else {
            if ((data & 8) != 0) {
                return new SimpleCollisionBox(0.0, 1.0 - var2, 0.0, 1.0, 1.0, 1.0);
            } else {
                return new SimpleCollisionBox(0.0, 0.0, 0.0, 1.0, var2, 1.0);
            }
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
