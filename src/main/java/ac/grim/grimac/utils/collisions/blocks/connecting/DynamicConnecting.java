package ac.grim.grimac.utils.collisions.blocks.connecting;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.blockdata.WrappedBlockData;
import ac.grim.grimac.utils.blockdata.types.WrappedFenceGate;
import ac.grim.grimac.utils.blockdata.types.WrappedStairs;
import ac.grim.grimac.utils.blockstate.BaseBlockState;
import ac.grim.grimac.utils.collisions.datatypes.*;
import ac.grim.grimac.utils.nmsImplementations.Materials;
import ac.grim.grimac.utils.nmsImplementations.XMaterial;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import io.github.retrooper.packetevents.utils.server.ServerVersion;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;

public class DynamicConnecting {
    private static final Material BARRIER = XMaterial.BARRIER.parseMaterial();
    private static final Material CARVED_PUMPKIN = XMaterial.CARVED_PUMPKIN.parseMaterial();
    private static final Material JACK_O_LANTERN = XMaterial.JACK_O_LANTERN.parseMaterial();
    private static final Material PUMPKIN = XMaterial.PUMPKIN.parseMaterial();
    private static final Material MELON = XMaterial.MELON.parseMaterial();
    private static final Material BEACON = XMaterial.BEACON.parseMaterial();
    private static final Material GLOWSTONE = XMaterial.GLOWSTONE.parseMaterial();
    private static final Material SEA_LANTERN = XMaterial.SEA_LANTERN.parseMaterial();
    private static final Material ICE = XMaterial.ICE.parseMaterial();

    private static final Material PISTON = XMaterial.PISTON.parseMaterial();
    private static final Material STICKY_PISTON = XMaterial.STICKY_PISTON.parseMaterial();
    private static final Material PISTON_HEAD = XMaterial.PISTON_HEAD.parseMaterial();

    public static CollisionBox[] makeShapes(float p_196408_1_, float p_196408_2_, float p_196408_3_, float p_196408_4_, float p_196408_5_, boolean includeCenter) {
        float middleMin = 8.0F - p_196408_1_;
        float middleMax = 8.0F + p_196408_1_;
        float f2 = 8.0F - p_196408_2_;
        float f3 = 8.0F + p_196408_2_;
        SimpleCollisionBox up = new HexCollisionBox(middleMin, 0.0D, middleMin, middleMax, p_196408_3_, middleMax);
        SimpleCollisionBox voxelshape1 = new HexCollisionBox(f2, p_196408_4_, 0.0D, f3, p_196408_5_, f3);
        SimpleCollisionBox voxelshape2 = new HexCollisionBox(f2, p_196408_4_, f2, f3, p_196408_5_, 16.0D);
        SimpleCollisionBox voxelshape3 = new HexCollisionBox(0.0D, p_196408_4_, f2, f3, p_196408_5_, f3);
        SimpleCollisionBox voxelshape4 = new HexCollisionBox(f2, p_196408_4_, f2, 16.0D, p_196408_5_, f3);

        ComplexCollisionBox voxelshape5 = new ComplexCollisionBox(voxelshape1, voxelshape4);
        ComplexCollisionBox voxelshape6 = new ComplexCollisionBox(voxelshape2, voxelshape3);

        CollisionBox[] avoxelshape = new CollisionBox[]{NoCollisionBox.INSTANCE, voxelshape2, voxelshape3, voxelshape6, voxelshape1, new ComplexCollisionBox(voxelshape2, voxelshape1), new ComplexCollisionBox(voxelshape3, voxelshape1), new ComplexCollisionBox(voxelshape6, voxelshape1), voxelshape4, new ComplexCollisionBox(voxelshape2, voxelshape4), new ComplexCollisionBox(voxelshape3, voxelshape4), new ComplexCollisionBox(voxelshape6, voxelshape4), voxelshape5, new ComplexCollisionBox(voxelshape2, voxelshape5), new ComplexCollisionBox(voxelshape3, voxelshape5), new ComplexCollisionBox(voxelshape6, voxelshape5)};

        if (includeCenter) {
            for (int i = 0; i < 16; ++i) {
                avoxelshape[i] = new ComplexCollisionBox(up, avoxelshape[i]);
            }
        }

        return avoxelshape;
    }

    public boolean connectsTo(GrimPlayer player, ClientVersion v, int currX, int currY, int currZ, BlockFace direction) {
        BaseBlockState targetBlock = player.compensatedWorld.getWrappedBlockStateAt(currX + direction.getModX(), currY + direction.getModY(), currZ + direction.getModZ());
        BaseBlockState currBlock = player.compensatedWorld.getWrappedBlockStateAt(currX, currY, currZ);
        Material target = targetBlock.getMaterial();
        Material fence = currBlock.getMaterial();

        if (!Materials.checkFlag(target, Materials.FENCE) && isBlacklisted(target))
            return false;

        // 1.9-1.11 clients don't have BARRIER exemption
        // https://bugs.mojang.com/browse/MC-9565
        if (target == BARRIER) return player.getClientVersion().isOlderThanOrEquals(ClientVersion.v_1_7_10) ||
                player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_9) &&
                        player.getClientVersion().isOlderThanOrEquals(ClientVersion.v_1_11_1);

        if (Materials.checkFlag(target, Materials.STAIRS)) {
            // 1.12 clients generate their own data, 1.13 clients use the server's data
            // 1.11- versions don't allow fences to connect to the back sides of stairs
            if (v.isOlderThan(ClientVersion.v_1_12) || (ServerVersion.getVersion().isOlderThanOrEquals(ServerVersion.v_1_11) && v.isNewerThanOrEquals(ClientVersion.v_1_13)))
                return false;
            WrappedStairs stairs = (WrappedStairs) WrappedBlockData.getMaterialData(targetBlock);

            return stairs.getDirection() == direction;
        } else if (canConnectToGate() && Materials.checkFlag(target, Materials.GATE)) {
            WrappedFenceGate gate = (WrappedFenceGate) WrappedBlockData.getMaterialData(targetBlock);
            // 1.4-1.11 clients don't check for fence gate direction
            // https://bugs.mojang.com/browse/MC-94016
            if (v.isOlderThanOrEquals(ClientVersion.v_1_11_1)) return true;

            BlockFace f1 = gate.getDirection();
            BlockFace f2 = f1.getOppositeFace();
            return direction == f1 || direction == f2;
        } else {
            if (fence == target) return true;

            return checkCanConnect(player, targetBlock, target, fence);
        }
    }

    boolean isBlacklisted(Material m) {
        if (Materials.checkFlag(m, Materials.LEAVES)) return true;
        if (Materials.checkFlag(m, Materials.SHULKER)) return true;
        if (Materials.checkFlag(m, Materials.TRAPDOOR)) return true;


        return m == CARVED_PUMPKIN || m == JACK_O_LANTERN || m == PUMPKIN || m == MELON ||
                m == BEACON || Materials.checkFlag(m, Materials.CAULDRON) || m == GLOWSTONE || m == SEA_LANTERN || m == ICE
                || m == PISTON || m == STICKY_PISTON || m == PISTON_HEAD || !canConnectToGlassBlock() && Materials.checkFlag(m, Materials.GLASS_BLOCK);
    }

    protected int getAABBIndex(boolean north, boolean east, boolean south, boolean west) {
        int i = 0;

        if (north) {
            i |= 1 << 2;
        }

        if (east) {
            i |= 1 << 3;
        }

        if (south) {
            i |= 1;
        }

        if (west) {
            i |= 1 << 1;
        }

        return i;
    }


    public boolean checkCanConnect(GrimPlayer player, BaseBlockState state, Material one, Material two) {
        return false;
    }

    public boolean canConnectToGlassBlock() {
        return false;
    }

    public boolean canConnectToGate() {
        return true;
    }
}
