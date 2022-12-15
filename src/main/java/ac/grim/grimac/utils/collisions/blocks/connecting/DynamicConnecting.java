package ac.grim.grimac.utils.collisions.blocks.connecting;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.*;
import ac.grim.grimac.utils.nmsutil.Materials;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.defaulttags.BlockTags;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;

public class DynamicConnecting {

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
        WrappedBlockState targetBlock = player.compensatedWorld.getWrappedBlockStateAt(currX + direction.getModX(), currY + direction.getModY(), currZ + direction.getModZ());
        WrappedBlockState currBlock = player.compensatedWorld.getWrappedBlockStateAt(currX, currY, currZ);
        StateType target = targetBlock.getType();
        StateType fence = currBlock.getType();

        if (!BlockTags.FENCES.contains(target) && isBlacklisted(target, fence, v))
            return false;

        // 1.12+ clients can connect to TnT while previous versions can't
        if (target == StateTypes.TNT)
            return v.isNewerThanOrEquals(ClientVersion.V_1_12);

        // 1.9-1.11 clients don't have BARRIER exemption
        // https://bugs.mojang.com/browse/MC-9565
        if (target == StateTypes.BARRIER)
            return player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_7_10) ||
                    player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9) &&
                            player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_11_1);

        if (BlockTags.STAIRS.contains(target)) {
            // 1.12 clients generate their own data, 1.13 clients use the server's data
            // 1.11- versions don't allow fences to connect to the back sides of stairs
            if (v.isOlderThan(ClientVersion.V_1_12) || (PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_11) && v.isNewerThanOrEquals(ClientVersion.V_1_13)))
                return false;
            return targetBlock.getFacing().getOppositeFace() == direction;
        } else if (canConnectToGate(fence) && BlockTags.FENCE_GATES.contains(target)) {
            // 1.4-1.11 clients don't check for fence gate direction
            // https://bugs.mojang.com/browse/MC-94016
            if (v.isOlderThanOrEquals(ClientVersion.V_1_11_1)) return true;

            BlockFace f1 = targetBlock.getFacing();
            BlockFace f2 = f1.getOppositeFace();
            return direction != f1 && direction != f2;
        } else {
            if (fence == target) return true;

            return checkCanConnect(player, targetBlock, target, fence);
        }
    }

    boolean isBlacklisted(StateType m, StateType fence, ClientVersion clientVersion) {
        if (BlockTags.LEAVES.contains(m)) return clientVersion.isNewerThan(ClientVersion.V_1_8) || !Materials.isGlassPane(fence);
        if (BlockTags.SHULKER_BOXES.contains(m)) return true;
        if (BlockTags.TRAPDOORS.contains(m)) return true;

        return m == StateTypes.CARVED_PUMPKIN || m == StateTypes.JACK_O_LANTERN || m == StateTypes.PUMPKIN || m == StateTypes.MELON ||
                m == StateTypes.BEACON || BlockTags.CAULDRONS.contains(m) || m == StateTypes.GLOWSTONE || m == StateTypes.SEA_LANTERN || m == StateTypes.ICE
                || m == StateTypes.PISTON || m == StateTypes.STICKY_PISTON || m == StateTypes.PISTON_HEAD || (!canConnectToGlassBlock()
                && BlockTags.GLASS_BLOCKS.contains(m));
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

    public boolean checkCanConnect(GrimPlayer player, WrappedBlockState state, StateType one, StateType two) {
        return false;
    }

    public boolean canConnectToGlassBlock() {
        return false;
    }

    public boolean canConnectToGate(StateType fence) {
        return !Materials.isGlassPane(fence);
    }
}