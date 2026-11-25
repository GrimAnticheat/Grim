package ac.grim.grimac.utils.nmsutil;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.IndexedVector3d;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import ac.grim.grimac.utils.math.GrimMath;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.potion.PotionTypes;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;

public class StuckSpeed {

    public static final IndexedVector3d NONE = new IndexedVector3d("none", 0, 1, 1, 1);
    public static final IndexedVector3d COBWEB = new IndexedVector3d("cobweb", 1, 0.25, 0.05F, 0.25);
    public static final IndexedVector3d COBWEB_WEAVING = new IndexedVector3d("cobweb_waving", 1 << 1, 0.5, 0.25, 0.5);
    public static final IndexedVector3d SWEET_BERRY_BUSH = new IndexedVector3d("sweet_berry_bush", 1 << 2, 0.8F, 0.75, 0.8F);
    public static final IndexedVector3d POWDER_SNOW = new IndexedVector3d("powder_snow", 1 << 3, 0.9F, 1.5, 0.9F);
    public static final IndexedVector3d[] POSSIBILITIES = {
            COBWEB,
            COBWEB_WEAVING,
            SWEET_BERRY_BUSH,
            POWDER_SNOW
    };

    // 0.03 hack
    public static int checkStuckSpeed(GrimPlayer player, double expand) {
        // Use the bounding box for after the player's movement is applied
        SimpleCollisionBox box = GetBoundingBox.getCollisionBoxForPlayer(player, player.x, player.y, player.z).expand(expand);

        int minX = GrimMath.mojangFloor(box.minX);
        int minY = GrimMath.mojangFloor(box.minY);
        int minZ = GrimMath.mojangFloor(box.minZ);
        int maxX = GrimMath.mojangFloor(box.maxX);
        int maxY = GrimMath.mojangFloor(box.maxY);
        int maxZ = GrimMath.mojangFloor(box.maxZ);

        if (player.compensatedWorld.areChunksUnloadedAt(minX, minY, minZ, maxX, maxY, maxZ))
            return StuckSpeed.NONE.getIndex();

        PacketEntity riding = player.compensatedEntities.self.getRiding();
        boolean stuckEntityIsLiving = riding == null || riding.isLivingEntity;
        int stuckSpeedMask = StuckSpeed.NONE.getIndex();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    WrappedBlockState block = player.compensatedWorld.getBlock(x, y, z);
                    StateType blockType = block.getType();

                    if (blockType == StateTypes.COBWEB) {
                        if (stuckEntityIsLiving && player.compensatedEntities.hasPotionEffect(PotionTypes.WEAVING)) {
                            stuckSpeedMask |= StuckSpeed.COBWEB_WEAVING.getIndex();
                        } else {
                            stuckSpeedMask |= StuckSpeed.COBWEB.getIndex();
                        }
                    }

                    if (stuckEntityIsLiving && blockType == StateTypes.SWEET_BERRY_BUSH && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_14)) {
                        stuckSpeedMask |= StuckSpeed.SWEET_BERRY_BUSH.getIndex();
                    }

                    if (blockType == StateTypes.POWDER_SNOW && x == Math.floor(player.x) && y == Math.floor(player.y) && z == Math.floor(player.z) && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_17)) {
                        stuckSpeedMask |= StuckSpeed.POWDER_SNOW.getIndex();
                    }
                }
            }
        }

        return stuckSpeedMask;
    }

}
