package ac.grim.grimac.utils.collisions.blocks;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.blockdata.WrappedBlockDataValue;
import ac.grim.grimac.utils.collisions.CollisionBox;
import ac.grim.grimac.utils.collisions.types.CollisionFactory;
import ac.grim.grimac.utils.collisions.types.ComplexCollisionBox;
import ac.grim.grimac.utils.collisions.types.SimpleCollisionBox;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import org.bukkit.block.data.BlockData;

public class PistonHeadCollision implements CollisionFactory {
    public static final int[] offsetsXForSide = new int[]{0, 0, 0, 0, -1, 1};

    public static int clamp_int(int p_76125_0_, int p_76125_1_, int p_76125_2_) {
        return p_76125_0_ < p_76125_1_ ? p_76125_1_ : (p_76125_0_ > p_76125_2_ ? p_76125_2_ : p_76125_0_);
    }

    public CollisionBox fetch(ClientVersion version, byte data, int x, int y, int z) {
        //byte data = block.getState().getData().getData();

        switch (clamp_int(data & 7, 0, offsetsXForSide.length - 1)) {
            case 0:
                return new ComplexCollisionBox(new SimpleCollisionBox(0.0F, 0.0F, 0.0F, 1.0F, 0.25F, 1.0F),
                        new SimpleCollisionBox(0.375F, 0.25F, 0.375F, 0.625F, 1.0F, 0.625F));
            case 1:
                return new ComplexCollisionBox(new SimpleCollisionBox(0.0F, 0.75F, 0.0F, 1.0F, 1.0F, 1.0F),
                        new SimpleCollisionBox(0.375F, 0.0F, 0.375F, 0.625F, 0.75F, 0.625F));
            case 2:
                return new ComplexCollisionBox(new SimpleCollisionBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 0.25F),
                        new SimpleCollisionBox(0.25F, 0.375F, 0.25F, 0.75F, 0.625F, 1.0F));
            case 3:
                return new ComplexCollisionBox(new SimpleCollisionBox(0.0F, 0.0F, 0.75F, 1.0F, 1.0F, 1.0F),
                        new SimpleCollisionBox(0.25F, 0.375F, 0.0F, 0.75F, 0.625F, 0.75F));
            case 4:
                return new ComplexCollisionBox(new SimpleCollisionBox(0.0F, 0.0F, 0.0F, 0.25F, 1.0F, 1.0F),
                        new SimpleCollisionBox(0.375F, 0.25F, 0.25F, 0.625F, 0.75F, 1.0F));
            case 5:
                return new ComplexCollisionBox(new SimpleCollisionBox(0.75F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F),
                        new SimpleCollisionBox(0.0F, 0.375F, 0.25F, 0.75F, 0.625F, 0.75F));
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
