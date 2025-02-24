package ac.grim.grimac.utils.collisions.datatypes;

import ac.grim.grimac.utils.math.GrimMath;
import com.github.retrooper.packetevents.protocol.world.states.defaulttags.BlockTags;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;

import java.util.HashSet;

public class OffsetCollisionBox extends SimpleCollisionBox {

    public static enum OffsetType {
        NONE,
        XZ,
        XYZ,
    }

    float maxHorizontalModelOffset = 0.25F;
    float maxVerticalModelOffset = 0.2F;
    double offsetX = 0;
    double offsetY = 0;
    double offsetZ = 0;

    OffsetType offsetType;

    private static final HashSet<StateType> XZ_OFFSET_BLOCKSTATES = new HashSet<>();
    private static final HashSet<StateType> XYZ_OFFSET_BLOCKSTATES = new HashSet<>();

    static {
        // Can we add a hasOffSet to StateType() ?
        // Or a new BlockTag for XZ and XYZ Offset ?
        XZ_OFFSET_BLOCKSTATES.add(StateTypes.MANGROVE_PROPAGULE);

        XZ_OFFSET_BLOCKSTATES.addAll(BlockTags.SMALL_FLOWERS.getStates());
        XZ_OFFSET_BLOCKSTATES.add(StateTypes.BAMBOO_SAPLING);
        XZ_OFFSET_BLOCKSTATES.add(StateTypes.BAMBOO);
        XZ_OFFSET_BLOCKSTATES.add(StateTypes.POINTED_DRIPSTONE);
        // Only offsets rendering HitBox on XZ // we should document this somewhere for future reference
//        XZ_OFFSET_BLOCKSTATES.addAll(BlockTags.TALL_FLOWERS.getStates());
//        XZ_OFFSET_BLOCKSTATES.add(StateTypes.TALL_SEAGRASS);
//        XZ_OFFSET_BLOCKSTATES.add(StateTypes.TALL_GRASS);
//        XZ_OFFSET_BLOCKSTATES.add(StateTypes.LARGE_FERN);
//        XZ_OFFSET_BLOCKSTATES.add(StateTypes.WARPED_ROOTS);
//        XZ_OFFSET_BLOCKSTATES.add(StateTypes.NETHER_SPROUTS);
//        XZ_OFFSET_BLOCKSTATES.add(StateTypes.CRIMSON_ROOTS);
//        XZ_OFFSET_BLOCKSTATES.add(StateTypes.HANGING_ROOTS);

        // Only offsets rendering on XYZ, not HitBox
//        XYZ_OFFSET_BLOCKSTATES.add(StateTypes.SHORT_GRASS);
//        XYZ_OFFSET_BLOCKSTATES.add(StateTypes.FERN);
//        XYZ_OFFSET_BLOCKSTATES.add(StateTypes.SMALL_DRIPLEAF);
    }

    public OffsetCollisionBox(StateType block, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        super(minX, minY, minZ, maxX, maxY, maxZ);
        if (block.equals(StateTypes.POINTED_DRIPSTONE)) {
            maxHorizontalModelOffset = 0.125F;
        }
//        else if (block.equals(StateTypes.SMALL_DRIPLEAF)) {
//            maxVerticalModelOffset = 0.1F;
//        }

        if (XZ_OFFSET_BLOCKSTATES.contains(block)) {
            offsetType = OffsetType.XZ;
            return;
        } else if (XYZ_OFFSET_BLOCKSTATES.contains(block)) {
            offsetType = OffsetType.XYZ;
            return;
        }
        throw new RuntimeException("Invalid State Type for OffSetCollisionBox: " + block);
    }

    @Override
    public SimpleCollisionBox offset(double x, double y, double z) {
        // In case you want to call .offset() again or get the box values without offset.
        resetBlockStateOffSet();
        long l;
        switch (offsetType) {
            case NONE:
                return super.offset(x, y, z);
            case XZ:
                l = GrimMath.hashCode(x, 0, z);
                offsetX = GrimMath.clamp(((double)((float)(l & 15L) / 15.0F) - 0.5) * 0.5, -maxHorizontalModelOffset, maxHorizontalModelOffset);
                offsetZ = GrimMath.clamp(((double)((float)(l >> 8 & 15L) / 15.0F) - 0.5) * 0.5, -maxHorizontalModelOffset, maxHorizontalModelOffset);
                return super.offset(x + offsetX, y, z + offsetZ);
            case XYZ:
                l = GrimMath.hashCode(x, 0, z);
                offsetY = ((double)((float)(l >> 4 & 15L) / 15.0F) - 1.0) * (double) maxVerticalModelOffset;
                offsetX = GrimMath.clamp(((double)((float)(l & 15L) / 15.0F) - 0.5) * 0.5, -maxHorizontalModelOffset, maxHorizontalModelOffset);
                offsetZ = GrimMath.clamp(((double)((float)(l >> 8 & 15L) / 15.0F) - 0.5) * 0.5, -maxHorizontalModelOffset, maxHorizontalModelOffset);
                return super.offset(x + offsetX, offsetY, z + offsetZ);
        }
        // You *really* shouldn't be using this class if offsetType = null
        return null;
    }

    public void resetBlockStateOffSet() {
        this.minX += offsetX;
        this.minY += offsetY;
        this.minZ += offsetZ;
        this.maxX += offsetX;
        this.maxY += offsetY;
        this.maxZ += offsetZ;
    }
}
