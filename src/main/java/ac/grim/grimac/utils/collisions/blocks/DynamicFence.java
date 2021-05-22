package ac.grim.grimac.utils.collisions.blocks;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.blockdata.WrappedBlockData;
import ac.grim.grimac.utils.blockdata.types.WrappedBlockDataValue;
import ac.grim.grimac.utils.blockdata.types.WrappedFenceGate;
import ac.grim.grimac.utils.blockdata.types.WrappedMultipleFacing;
import ac.grim.grimac.utils.blockdata.types.WrappedStairs;
import ac.grim.grimac.utils.blockstate.BaseBlockState;
import ac.grim.grimac.utils.collisions.datatypes.CollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.CollisionFactory;
import ac.grim.grimac.utils.collisions.datatypes.ComplexCollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.nmsImplementations.Materials;
import ac.grim.grimac.utils.nmsImplementations.XMaterial;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;

public class DynamicFence implements CollisionFactory {
    // TODO: 1.9-1.11 clients don't have BARRIER exemption
    // https://bugs.mojang.com/browse/MC-9565
    // TODO: 1.4-1.11 clients don't check for fence gate direction
    // https://bugs.mojang.com/browse/MC-94016

    private static final double width = 0.125;
    private static final double min = .5 - width;
    private static final double max = .5 + width;

    private static final Material BARRIER = XMaterial.BARRIER.parseMaterial();
    private static final Material CARVED_PUMPKIN = XMaterial.CARVED_PUMPKIN.parseMaterial();
    private static final Material JACK_O_LANTERN = XMaterial.JACK_O_LANTERN.parseMaterial();
    private static final Material PUMPKIN = XMaterial.PUMPKIN.parseMaterial();
    private static final Material MELON = XMaterial.MELON.parseMaterial();
    private static final Material BEACON = XMaterial.BEACON.parseMaterial();
    private static final Material CAULDRON = XMaterial.CAULDRON.parseMaterial();
    private static final Material GLOWSTONE = XMaterial.GLOWSTONE.parseMaterial();
    private static final Material SEA_LANTERN = XMaterial.SEA_LANTERN.parseMaterial();
    private static final Material ICE = XMaterial.ICE.parseMaterial();

    private static final Material NETHER_BRICK_FENCE = XMaterial.NETHER_BRICK_FENCE.parseMaterial();

    static boolean isBlacklisted(Material m) {
        if (Materials.checkFlag(m, Materials.LEAVES)) return true;
        if (Materials.checkFlag(m, Materials.SHULKER)) return true;
        if (Materials.checkFlag(m, Materials.TRAPDOOR)) return true;


        return m == BARRIER || m == CARVED_PUMPKIN || m == JACK_O_LANTERN || m == PUMPKIN || m == MELON ||
                m == BEACON || m == CAULDRON || m == GLOWSTONE || m == SEA_LANTERN || m == ICE;
    }

    private static boolean fenceConnects(GrimPlayer player, ClientVersion v, int currX, int currY, int currZ, BlockFace direction) {
        BaseBlockState targetBlock = player.compensatedWorld.getWrappedBlockStateAt(currX + direction.getModX(), currY + direction.getModY(), currZ + direction.getModZ());
        BaseBlockState currBlock = player.compensatedWorld.getWrappedBlockStateAt(currX, currY, currZ);
        Material target = targetBlock.getMaterial();
        Material fence = currBlock.getMaterial();

        if (!Materials.checkFlag(target, Materials.FENCE) && isBlacklisted(target))
            return false;

        if (Materials.checkFlag(target, Materials.STAIRS)) {
            // 1.12 clients generate their own data, 1.13 clients use the server's data
            // 1.11- versions don't allow fences to connect to the back sides of stairs
            if (v.isOlderThan(ClientVersion.v_1_12) || (XMaterial.getVersion() < 12 && v.isNewerThanOrEquals(ClientVersion.v_1_13)))
                return false;
            WrappedStairs stairs = (WrappedStairs) WrappedBlockData.getMaterialData(target).getData(targetBlock);

            return stairs.getDirection() == direction;
        } else if (Materials.checkFlag(target, Materials.GATE)) {
            WrappedFenceGate gate = (WrappedFenceGate) WrappedBlockData.getMaterialData(target).getData(targetBlock);
            BlockFace f1 = gate.getDirection();
            BlockFace f2 = f1.getOppositeFace();
            return direction == f1 || direction == f2;
        } else {
            if (fence == target) return true;
            if (Materials.checkFlag(target, Materials.FENCE))
                return !(fence == NETHER_BRICK_FENCE) && !(target == NETHER_BRICK_FENCE);
            else return Materials.checkFlag(target, Materials.FENCE) || (target.isSolid() && !target.isTransparent());
        }
    }

    @Override
    public CollisionBox fetch(GrimPlayer player, ClientVersion version, WrappedBlockDataValue block, int x, int y, int z) {
        ComplexCollisionBox box = new ComplexCollisionBox(new SimpleCollisionBox(min, 0, min, max, 1.5, max));

        boolean east;
        boolean north;
        boolean south;
        boolean west;

        // 1.13+ servers on 1.13+ clients send the full fence data
        if (XMaterial.isNewVersion() && version.isNewerThanOrEquals(ClientVersion.v_1_13)) {
            WrappedMultipleFacing fence = (WrappedMultipleFacing) block;

            east = fence.getDirections().contains(BlockFace.EAST);
            north = fence.getDirections().contains(BlockFace.NORTH);
            south = fence.getDirections().contains(BlockFace.SOUTH);
            west = fence.getDirections().contains(BlockFace.WEST);
        } else {
            east = fenceConnects(player, version, x, y, z, BlockFace.EAST);
            north = fenceConnects(player, version, x, y, z, BlockFace.NORTH);
            south = fenceConnects(player, version, x, y, z, BlockFace.SOUTH);
            west = fenceConnects(player, version, x, y, z, BlockFace.WEST);
        }

        if (east) box.add(new SimpleCollisionBox(max, 0, min, 1, 1.5, max));
        if (west) box.add(new SimpleCollisionBox(0, 0, min, max, 1.5, max));
        if (north) box.add(new SimpleCollisionBox(min, 0, 0, max, 1.5, min));
        if (south) box.add(new SimpleCollisionBox(min, 0, max, max, 1.5, 1));
        return box;
    }
}
