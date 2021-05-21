package ac.grim.grimac.utils.collisions.blocks;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.blockdata.types.WrappedBlockDataValue;
import ac.grim.grimac.utils.blockdata.types.WrappedMultipleFacing;
import ac.grim.grimac.utils.collisions.datatypes.CollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.CollisionFactory;
import ac.grim.grimac.utils.collisions.datatypes.ComplexCollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import org.bukkit.block.BlockFace;

public class DynamicChorusPlant implements CollisionFactory {
    private static final BlockFace[] directions = new BlockFace[]{BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN};
    private static final CollisionBox[] modernShapes = makeShapes();

    private static CollisionBox[] makeShapes() {
        float f = 0.5F - (float) 0.3125;
        float f1 = 0.5F + (float) 0.3125;
        SimpleCollisionBox baseShape = new SimpleCollisionBox(f, f, f, f1, f1, f1);
        CollisionBox[] avoxelshape = new CollisionBox[directions.length];

        for (int i = 0; i < directions.length; ++i) {
            BlockFace direction = directions[i];
            avoxelshape[i] = new SimpleCollisionBox(0.5D + Math.min(-(float) 0.3125, (double) direction.getModX() * 0.5D), 0.5D + Math.min(-(float) 0.3125, (double) direction.getModY() * 0.5D), 0.5D + Math.min(-(float) 0.3125, (double) direction.getModZ() * 0.5D), 0.5D + Math.max((float) 0.3125, (double) direction.getModX() * 0.5D), 0.5D + Math.max((float) 0.3125, (double) direction.getModY() * 0.5D), 0.5D + Math.max((float) 0.3125, (double) direction.getModZ() * 0.5D));
        }

        CollisionBox[] avoxelshape1 = new CollisionBox[64];

        for (int k = 0; k < 64; ++k) {
            ComplexCollisionBox directionalShape = new ComplexCollisionBox(baseShape);

            for (int j = 0; j < directions.length; ++j) {
                if ((k & 1 << j) != 0) {
                    directionalShape.add(avoxelshape[j]);
                }
            }

            avoxelshape1[k] = directionalShape;
        }

        return avoxelshape1;
    }

    @Override
    public CollisionBox fetch(GrimPlayer player, ClientVersion version, WrappedBlockDataValue block, int x, int y, int z) {
        WrappedMultipleFacing facing = (WrappedMultipleFacing) block;

        return modernShapes[getAABBIndex(facing)].copy();
    }

    protected int getAABBIndex(WrappedMultipleFacing p_196486_1_) {
        int i = 0;

        for (int j = 0; j < directions.length; ++j) {
            if (p_196486_1_.getDirections().contains(directions[j])) {
                i |= 1 << j;
            }
        }

        return i;
    }
}
