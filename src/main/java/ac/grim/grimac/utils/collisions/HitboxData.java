package ac.grim.grimac.utils.collisions;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.blockdata.WrappedBlockData;
import ac.grim.grimac.utils.blockdata.types.WrappedBlockDataValue;
import ac.grim.grimac.utils.blockdata.types.WrappedFenceGate;
import ac.grim.grimac.utils.blockdata.types.WrappedFlatBlock;
import ac.grim.grimac.utils.blockdata.types.WrappedSnow;
import ac.grim.grimac.utils.blockstate.BaseBlockState;
import ac.grim.grimac.utils.collisions.blocks.connecting.DynamicWall;
import ac.grim.grimac.utils.collisions.datatypes.*;
import ac.grim.grimac.utils.nmsutil.Materials;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import org.bukkit.Material;
import org.bukkit.block.data.type.BigDripleaf;
import org.bukkit.block.data.type.Lectern;
import org.bukkit.block.data.type.Scaffolding;

import java.util.*;

// Expansion to the CollisionData class, which is different than regular ray tracing hitboxes
public enum HitboxData {
    SCAFFOLDING((player, item, version, data, x, y, z) -> {
        // If is holding scaffolding
        if (item == Material.SCAFFOLDING) {
            return new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true);
        }

        Scaffolding scaffolding = (Scaffolding) ((WrappedFlatBlock) data).getBlockData();

        // STABLE_SHAPE for the scaffolding
        ComplexCollisionBox box = new ComplexCollisionBox(
                new HexCollisionBox(0.0D, 14.0D, 0.0D, 16.0D, 16.0D, 16.0D),
                new HexCollisionBox(0.0D, 0.0D, 0.0D, 2.0D, 16.0D, 2.0D),
                new HexCollisionBox(14.0D, 0.0D, 0.0D, 16.0D, 16.0D, 2.0D),
                new HexCollisionBox(0.0D, 0.0D, 14.0D, 2.0D, 16.0D, 16.0D),
                new HexCollisionBox(14.0D, 0.0D, 14.0D, 16.0D, 16.0D, 16.0D));

        if (scaffolding.isBottom()) { // Add the unstable shape to the collision boxes
            box.add(new HexCollisionBox(0.0D, 0.0D, 0.0D, 2.0D, 2.0D, 16.0D));
            box.add(new HexCollisionBox(14.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D));
            box.add(new HexCollisionBox(0.0D, 0.0D, 14.0D, 16.0D, 2.0D, 16.0D));
            box.add(new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 2.0D));
        }

        return box;
    }, ItemTypes.SCAFFOLDING),

    DRIPLEAF((player, item, version, data, x, y, z) -> {
        if (version.isOlderThanOrEquals(ClientVersion.V_1_16_4))
            return new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true);

        BigDripleaf dripleaf = (BigDripleaf) ((WrappedFlatBlock) data).getBlockData();

        ComplexCollisionBox box = new ComplexCollisionBox();

        if (dripleaf.getFacing() == org.bukkit.block.BlockFace.NORTH) { // Stem
            box.add(new HexCollisionBox(5.0D, 0.0D, 9.0D, 11.0D, 15.0D, 15.0D));
        } else if (dripleaf.getFacing() == org.bukkit.block.BlockFace.SOUTH) {
            box.add(new HexCollisionBox(5.0D, 0.0D, 1.0D, 11.0D, 15.0D, 7.0D));
        } else if (dripleaf.getFacing() == org.bukkit.block.BlockFace.EAST) {
            box.add(new HexCollisionBox(1.0D, 0.0D, 5.0D, 7.0D, 15.0D, 11.0D));
        } else {
            box.add(new HexCollisionBox(9.0D, 0.0D, 5.0D, 15.0D, 15.0D, 11.0D));
        }

        if (dripleaf.getTilt() == BigDripleaf.Tilt.NONE || dripleaf.getTilt() == BigDripleaf.Tilt.UNSTABLE) {
            box.add(new HexCollisionBox(0.0, 11.0, 0.0, 16.0, 15.0, 16.0));
        } else if (dripleaf.getTilt() == BigDripleaf.Tilt.PARTIAL) {
            box.add(new HexCollisionBox(0.0, 11.0, 0.0, 16.0, 13.0, 16.0));
        }

        return box;

    }, ItemTypes.BIG_DRIPLEAF),

    FENCE_GATE((player, item, version, data, x, y, z) -> {
        WrappedFenceGate gate = (WrappedFenceGate) data;

        // This technically should be taken from the block data/made multi-version/run block updates... but that's too far even for me
        // This way is so much easier and works unless the magic stick wand is used
        boolean isInWall;
        boolean isXAxis = gate.getDirection() == BlockFace.WEST || gate.getDirection() == BlockFace.EAST;
        if (isXAxis) {
            boolean zPosWall = Materials.checkFlag(player.compensatedWorld.getBukkitMaterialAt(x, y, z + 1), Materials.WALL);
            boolean zNegWall = Materials.checkFlag(player.compensatedWorld.getBukkitMaterialAt(x, y, z - 1), Materials.WALL);
            isInWall = zPosWall || zNegWall;
        } else {
            boolean xPosWall = Materials.checkFlag(player.compensatedWorld.getBukkitMaterialAt(x + 1, y, z), Materials.WALL);
            boolean xNegWall = Materials.checkFlag(player.compensatedWorld.getBukkitMaterialAt(x - 1, y, z), Materials.WALL);
            isInWall = xPosWall || xNegWall;
        }

        if (isInWall) {
            return isXAxis ? new HexCollisionBox(6.0D, 0.0D, 0.0D, 10.0D, 13.0D, 16.0D) : new HexCollisionBox(0.0D, 0.0D, 6.0D, 16.0D, 13.0D, 10.0D);
        }

        return isXAxis ? new HexCollisionBox(6.0D, 0.0D, 0.0D, 10.0D, 16.0D, 16.0D) : new HexCollisionBox(0.0D, 0.0D, 6.0D, 16.0D, 16.0D, 10.0D);
    }, Arrays.stream(Material.values()).filter(mat -> mat.name().contains("FENCE") && mat.name().contains("GATE"))
            .toArray(Material[]::new)),

    FENCE((player, item, version, data, x, y, z) -> {
        BaseBlockState state = player.compensatedWorld.getWrappedBlockStateAt(x, y, z);

        ComplexCollisionBox collisionData = (ComplexCollisionBox) CollisionData.getData(state.getMaterial()).getMovementCollisionBox(player, version, state, x, y, z);

        List<SimpleCollisionBox> boxes = new ArrayList<>();
        collisionData.downCast(boxes);

        for (SimpleCollisionBox box : boxes) {
            box.maxY = 1;
        }

        return collisionData;
    }, Arrays.stream(Material.values()).filter(mat -> mat.name().contains("FENCE") && !mat.name().contains("GATE") && !mat.name().contains("IRON_FENCE"))
            .toArray(Material[]::new)),

    WALL((player, item, version, data, x, y, z) -> {
        BaseBlockState state = player.compensatedWorld.getWrappedBlockStateAt(x, y, z);
        return new DynamicWall().fetchRegularBox(player, state, version, x, y, z);
    }, Arrays.stream(Material.values()).filter(mat -> mat.name().contains("WALL")
            && !mat.name().contains("SIGN") && !mat.name().contains("HEAD") && !mat.name().contains("BANNER")
            && !mat.name().contains("FAN") && !mat.name().contains("SKULL") && !mat.name().contains("TORCH")).toArray(Material[]::new)),

    HONEY_BLOCK(new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true), ItemTypes.HONEY_BLOCK),

    POWDER_SNOW(new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true), ItemTypes.POWDER_SNOW),

    SOUL_SAND(new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true), ItemTypes.SOUL_SAND),

    CACTUS(new HexCollisionBox(1.0D, 0.0D, 1.0D, 15.0D, 16.0D, 15.0D), ItemTypes.CACTUS),

    SNOW((player, item, version, data, x, y, z) -> {
        WrappedSnow snow = (WrappedSnow) data;

        return new SimpleCollisionBox(0, 0, 0, 1, (snow.getLayers() + 1) * 0.125, 1);
    }, ItemTypes.SNOW),

    LECTERN_BLOCK((player, item, version, data, x, y, z) -> {
        ComplexCollisionBox common = new ComplexCollisionBox(new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D),
                new HexCollisionBox(4.0D, 2.0D, 4.0D, 12.0D, 14.0D, 12.0D));

        Lectern lectern = (Lectern) ((WrappedFlatBlock) data).getBlockData();

        if (lectern.getFacing() == org.bukkit.block.BlockFace.WEST) {
            common.add(new HexCollisionBox(1.0D, 10.0D, 0.0D, 5.333333D, 14.0D, 16.0D));
            common.add(new HexCollisionBox(5.333333D, 12.0D, 0.0D, 9.666667D, 16.0D, 16.0D));
            common.add(new HexCollisionBox(9.666667D, 14.0D, 0.0D, 14.0D, 18.0D, 16.0D));
        } else if (lectern.getFacing() == org.bukkit.block.BlockFace.NORTH) {
            common.add(new HexCollisionBox(0.0D, 10.0D, 1.0D, 16.0D, 14.0D, 5.333333D));
            common.add(new HexCollisionBox(0.0D, 12.0D, 5.333333D, 16.0D, 16.0D, 9.666667D));
            common.add(new HexCollisionBox(0.0D, 14.0D, 9.666667D, 16.0D, 18.0D, 14.0D));
        } else if (lectern.getFacing() == org.bukkit.block.BlockFace.EAST) {
            common.add(new HexCollisionBox(10.666667D, 10.0D, 0.0D, 15.0D, 14.0D, 16.0D));
            common.add(new HexCollisionBox(6.333333D, 12.0D, 0.0D, 10.666667D, 16.0D, 16.0D));
            common.add(new HexCollisionBox(2.0D, 14.0D, 0.0D, 6.333333D, 18.0D, 16.0D));
        } else { // SOUTH
            common.add(new HexCollisionBox(0.0D, 10.0D, 10.666667D, 16.0D, 14.0D, 15.0D));
            common.add(new HexCollisionBox(0.0D, 12.0D, 6.333333D, 16.0D, 16.0D, 10.666667D));
            common.add(new HexCollisionBox(0.0D, 14.0D, 2.0D, 16.0D, 18.0D, 6.333333D));
        }

        return common;
    }, ItemTypes.LECTERN);

    private static final HitboxData[] lookup = new HitboxData[Material.values().length];

    static {
        for (HitboxData data : HitboxData.values()) {
            for (Material mat : data.materials) lookup[mat.ordinal()] = data;
        }
    }

    private final Material[] materials;
    private CollisionBox box;
    private HitBoxFactory dynamic;

    HitboxData(CollisionBox box, Material... materials) {
        this.box = box;
        Set<Material> mList = new HashSet<>(Arrays.asList(materials));
        mList.remove(null); // Sets can contain one null
        this.materials = mList.toArray(new Material[0]);
    }

    HitboxData(HitBoxFactory dynamic, Material... materials) {
        this.dynamic = dynamic;
        Set<Material> mList = new HashSet<>(Arrays.asList(materials));
        mList.remove(null); // Sets can contain one null
        this.materials = mList.toArray(new Material[0]);
    }

    public static HitboxData getData(Material material) {
        return lookup[material.ordinal()];
    }

    public static CollisionBox getBlockHitbox(GrimPlayer player, Material heldItem, ClientVersion version, BaseBlockState block, int x, int y, int z) {
        HitboxData data = getData(block.getMaterial());

        if (data == null) {
            // Fall back to collision boxes
            return CollisionData.getRawData(block.getMaterial()).getMovementCollisionBox(player, version, block, x, y, z);
        }

        // Simple collision box to override
        if (data.box != null)
            return data.box.copy().offset(x, y, z);

        // Dynamic collision box
        WrappedBlockDataValue value = WrappedBlockData.getMaterialData(block);

        // Allow this class to override collision boxes when they aren't the same as regular boxes
        return HitboxData.getData(block.getMaterial()).dynamic.fetch(player, heldItem, version, value, x, y, z);
    }
}
