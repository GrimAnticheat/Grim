package ac.grim.grimac.utils.collisions.blocks;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.blockdata.WrappedBlockDataValue;
import ac.grim.grimac.utils.collisions.CollisionBox;
import ac.grim.grimac.utils.collisions.Materials;
import ac.grim.grimac.utils.collisions.types.CollisionFactory;
import ac.grim.grimac.utils.collisions.types.ComplexCollisionBox;
import ac.grim.grimac.utils.collisions.types.SimpleCollisionBox;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

public class DynamicFence implements CollisionFactory {
    // https://bugs.mojang.com/browse/MC-9565
    // https://bugs.mojang.com/browse/MC-94016

    private static final double width = 0.125;
    private static final double min = .5 - width;
    private static final double max = .5 + width;

    static boolean isBlacklisted(Material m) {
        switch (m.ordinal()) {
            case 138:
            case 280:
            case 86:
            case 103:
            case 166:
                return true;
            default:
                return Materials.checkFlag(m, Materials.STAIRS)
                        || Materials.checkFlag(m, Materials.WALL)
                        || m.name().contains("DAYLIGHT")
                        || Materials.checkFlag(m, Materials.FENCE);
        }
    }

    private static boolean fenceConnects(ClientVersion v, int currX, int currY, int currZ, int x, int y, int z) {
        // TODO: Fix this method to use block cache
        return false;
        /*IBlockData blockDir = ChunkCache.getBlockDataAt(x, y, z);
        IBlockData currBlock = ChunkCache.getBlockDataAt(currX, currY, currZ);
        Material target = sTarget.getType();
        Material fence = sFence.getType();

        if (!isFence(target) && isBlacklisted(target))
            return false;

        if (Materials.checkFlag(target, Materials.STAIRS)) {
            if (v.isBelow(ProtocolVersion.V1_12)) return false;
            Stairs stairs = (Stairs) sTarget.getData();
            BlockDirectional blockDirDir = (BlockDirectional) blockDir.getBlock();
            return blockDirDir..getFacing() == direction;
        } else if (target.name().contains("GATE")) {
            Gate gate = (Gate) sTarget.getData();
            BlockFace f1 = gate.getFacing();
            BlockFace f2 = f1.getOppositeFace();
            return direction == f1 || direction == f2;
        } else {
            if (fence == target) return true;
            if (isFence(target))
                return !fence.name().contains("NETHER") && !target.name().contains("NETHER");
            else return isFence(target) || (target.isSolid() && !target.isTransparent());
        }*/
    }

    private static boolean isFence(Material material) {
        return Materials.checkFlag(material, Materials.FENCE) && material.name().contains("FENCE");
    }

    public CollisionBox fetch(ClientVersion version, byte b, int x, int y, int z) {
        ComplexCollisionBox box = new ComplexCollisionBox(new SimpleCollisionBox(min, 0, min, max, 1.5, max));
        boolean east = fenceConnects(version, x, y, z, x + 1, y, z);
        boolean north = fenceConnects(version, x, y, z, x, y, z - 1);
        boolean south = fenceConnects(version, x, y, z, x, y, z + 1);
        boolean west = fenceConnects(version, x, y, z, x - 1, y, z);
        if (east) box.add(new SimpleCollisionBox(max, 0, min, 1, 1.5, max));
        if (west) box.add(new SimpleCollisionBox(0, 0, min, max, 1.5, max));
        if (north) box.add(new SimpleCollisionBox(min, 0, 0, max, 1.5, min));
        if (south) box.add(new SimpleCollisionBox(min, 0, max, max, 1.5, 1));
        return box;
    }

    public CollisionBox fetch(ClientVersion version, BlockData block, int x, int y, int z) {
        return fetch(version, (byte) 0, x, y, z);
    }

    @Override
    public CollisionBox fetch(GrimPlayer player, ClientVersion version, WrappedBlockDataValue block, int x, int y, int z) {
        return null;
    }
}
