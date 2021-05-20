package ac.grim.grimac.utils.collisions.blocks;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.blockdata.types.WrappedBlockDataValue;
import ac.grim.grimac.utils.collisions.datatypes.CollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.CollisionFactory;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.material.Stairs;

@SuppressWarnings("Duplicates")
public class DynamicPane implements CollisionFactory {

    private static final double width = 0.0625;
    private static final double min = .5 - width;
    private static final double max = .5 + width;

    private static boolean fenceConnects(ClientVersion v, Block fenceBlock, BlockFace direction) {
        Block targetBlock = fenceBlock.getRelative(direction, 1);
        BlockState sFence = fenceBlock.getState();
        BlockState sTarget = targetBlock.getState();
        Material target = sTarget.getType();
        Material fence = sFence.getType();

        if (!isPane(target) && DynamicFence.isBlacklisted(target))
            return false;

        if (target.name().contains("STAIRS")) {
            if (v.isOlderThan(ClientVersion.v_1_12)) return false;
            Stairs stairs = (Stairs) sTarget.getData();
            return stairs.getFacing() == direction;
        } else return isPane(target) || (target.isSolid() && !target.isTransparent());
    }

    private static boolean isPane(Material m) {
        int id = m.getId();
        return id == 101 || id == 102 || id == 160;
    }

    public CollisionBox fetch(ClientVersion version, byte b, int x, int y, int z) {

        return new SimpleCollisionBox(0, 0, 0, 1, 1, 1);
        /*ComplexCollisionBox box = new ComplexCollisionBox(new SimpleCollisionBox(min, 0, min, max, 1, max));
        boolean east = fenceConnects(version, b, BlockFace.EAST);
        boolean north = fenceConnects(version, b, BlockFace.NORTH);
        boolean south = fenceConnects(version, b, BlockFace.SOUTH);
        boolean west = fenceConnects(version, b, BlockFace.WEST);

        if (version.isBelow(ProtocolVersion.V1_9) && !(east || north || south || west)) {
            east = true;
            west = true;
            north = true;
            south = true;
        }

        if (east) box.add(new SimpleCollisionBox(max, 0, min, 1, 1, max));
        if (west) box.add(new SimpleCollisionBox(0, 0, min, max, 1, max));
        if (north) box.add(new SimpleCollisionBox(min, 0, 0, max, 1, min));
        if (south) box.add(new SimpleCollisionBox(min, 0, max, max, 1, 1));
        return box;*/
    }

    public CollisionBox fetch(ClientVersion version, BlockData block, int x, int y, int z) {
        return new SimpleCollisionBox(0, 0, 0, 1, 1, 1);
    }

    @Override
    public CollisionBox fetch(GrimPlayer player, ClientVersion version, WrappedBlockDataValue block, int x, int y, int z) {
        return null;
    }
}
