package ac.grim.grimac.utils.nmsImplementations;

import ac.grim.grimac.utils.collisions.CollisionBox;
import ac.grim.grimac.utils.collisions.blocks.DynamicWall;
import ac.grim.grimac.utils.collisions.types.*;
import ac.grim.grimac.utils.data.ProtocolVersion;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.block.data.type.Slab;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public enum CollisionData {
    _VINE(new CollisionFactory() {
        @Override
        public CollisionBox fetch(ProtocolVersion version, byte data, int x, int y, int z) {
            switch (data) {
                // TODO: Vines attaching to the top of blocks probably doesn't affect much, but is client sided
                // ViaVersion doesn't allow placing stuff at the top on 1.12 servers
                // But... on 1.13+ clients on 1.12 servers... GUESS WHAT??
                // Vines don't attach to the top of blocks at all, even when they should!
                // Doesn't affect much but could break some interaction checks.

                // South
                case (1):
                    return new SimpleCollisionBox(0., 0., 0.9375, 1., 1., 1.);
                // West
                case (2):
                    return new SimpleCollisionBox(0., 0., 0., 0.0625, 1., 1.);
                // North
                case (4):
                    return new SimpleCollisionBox(0., 0., 0., 1., 1., 0.0625);
                // East
                case (8):
                    return new SimpleCollisionBox(0.9375, 0., 0., 1., 1., 1.);
            }
            return new SimpleCollisionBox(0, 0, 0, 1., 1., 1.);
        }

        @Override
        public CollisionBox fetch(ProtocolVersion version, BlockData block, int x, int y, int z) {
            MultipleFacing facing = (MultipleFacing) block;
            ComplexCollisionBox boxes = new ComplexCollisionBox();

            for (BlockFace face : facing.getFaces()) {
                if (face == BlockFace.SOUTH) {
                    boxes.add(new SimpleCollisionBox(0., 0., 0.9375, 1., 1., 1.));
                }

                if (face == BlockFace.WEST) {
                    boxes.add(new SimpleCollisionBox(0., 0., 0., 0.0625, 1., 1.));
                }

                if (face == BlockFace.NORTH) {
                    boxes.add(new SimpleCollisionBox(0., 0., 0., 1., 1., 0.0625));
                }

                if (face == BlockFace.EAST) {
                    boxes.add(new SimpleCollisionBox(0.9375, 0., 0., 1., 1., 1.));
                }
            }

            return boxes;
        }
    }, XMaterial.VINE.parseMaterial()),


    _LIQUID(new SimpleCollisionBox(0, 0, 0, 1f, 0.9f, 1f),
            XMaterial.WATER.parseMaterial(), XMaterial.LAVA.parseMaterial()),

    _BREWINGSTAND(new ComplexCollisionBox(
            new SimpleCollisionBox(0, 0, 0, 1, 0.125, 1),                      //base
            new SimpleCollisionBox(0.4375, 0.0, 0.4375, 0.5625, 0.875, 0.5625) //top
    ), XMaterial.BREWING_STAND.parseMaterial()),

    _RAIL(new SimpleCollisionBox(0, 0, 0, 1, 0.125, 0),
            XMaterial.RAIL.parseMaterial(), XMaterial.ACTIVATOR_RAIL.parseMaterial(),
            XMaterial.DETECTOR_RAIL.parseMaterial(), XMaterial.POWERED_RAIL.parseMaterial()),

    _ANVIL(new CollisionFactory() {
        // TODO: Some version increased the amount of bounding boxes of this block by an insane amount
        @Override
        public CollisionBox fetch(ProtocolVersion version, byte data, int x, int y, int z) {

            // Anvil collision box was changed in 1.13 to be more accurate
            // https://www.mcpk.wiki/wiki/Version_Differences
            // The base is 0.75×0.75, and its floor is 0.25b high.
            // The top is 1×0.625, and its ceiling is 0.375b low.
            if (version.isOrAbove(ProtocolVersion.V1_13)) {
                ComplexCollisionBox complexAnvil = new ComplexCollisionBox();
                // Base of the anvil
                complexAnvil.add(new HexCollisionBox(2, 0, 2, 14, 4, 14));

                switch (data & 0b01) {
                    // North and South top
                    case (0):
                        complexAnvil.add(new HexCollisionBox(4.0D, 4.0D, 3.0D, 12.0D, 5.0D, 13.0D));
                        complexAnvil.add(new HexCollisionBox(6.0D, 5.0D, 4.0D, 10.0D, 10.0D, 12.0D));
                        complexAnvil.add(new HexCollisionBox(3.0D, 10.0D, 0.0D, 13.0D, 16.0D, 16.0D));
                        // East and West top
                    case (1):
                        complexAnvil.add(new HexCollisionBox(3.0D, 4.0D, 4.0D, 13.0D, 5.0D, 12.0D));
                        complexAnvil.add(new HexCollisionBox(4.0D, 5.0D, 6.0D, 12.0D, 10.0D, 10.0D));
                        complexAnvil.add(new HexCollisionBox(0.0D, 10.0D, 3.0D, 16.0D, 16.0D, 13.0D));
                }

                return complexAnvil;
            } else {
                // Just a single solid collision box with 1.12
                switch (data & 0b01) {
                    // North and South
                    case (0):
                        return new SimpleCollisionBox(0.125F, 0.0F, 0.0F, 0.875F, 1.0F, 1.0F);
                    // East and West
                    case (1):
                        return new SimpleCollisionBox(0.0F, 0.0F, 0.125F, 1.0F, 1.0F, 0.875F);
                }
            }

            // This should never run.
            return new SimpleCollisionBox(0, 0, 0, 1, 1, 1);
        }

        @Override
        public CollisionBox fetch(ProtocolVersion version, BlockData block, int x, int y, int z) {
            Directional facing = (Directional) block;

            // Making exemption for not using legacy stuff in modern stuff, as we are doing our own byte conversion
            if (facing.getFacing() == BlockFace.EAST || facing.getFacing() == BlockFace.WEST) {
                return fetch(version, (byte) 1, x, y, z);
            } else {
                // Must be North, South, or a bad server jar
                return fetch(version, (byte) 0, x, y, z);
            }
        }
    }, XMaterial.ANVIL.parseMaterial(), XMaterial.CHIPPED_ANVIL.parseMaterial(), XMaterial.DAMAGED_ANVIL.parseMaterial()),


    _WALL(new DynamicWall(), XMaterial.ANDESITE_WALL.parseMaterial(), XMaterial.BRICK_WALL.parseMaterial(),
            XMaterial.COBBLESTONE_WALL.parseMaterial(), XMaterial.DIORITE_WALL.parseMaterial(),
            XMaterial.END_STONE_BRICK_WALL.parseMaterial(), XMaterial.GRANITE_WALL.parseMaterial(),
            XMaterial.MOSSY_COBBLESTONE_WALL.parseMaterial(), XMaterial.MOSSY_STONE_BRICK_WALL.parseMaterial(),
            XMaterial.NETHER_BRICK_WALL.parseMaterial(), XMaterial.POLISHED_BLACKSTONE_BRICK_WALL.parseMaterial(),
            XMaterial.POLISHED_BLACKSTONE_WALL.parseMaterial(), XMaterial.PRISMARINE_WALL.parseMaterial(),
            XMaterial.RED_NETHER_BRICK_WALL.parseMaterial(), XMaterial.SANDSTONE_WALL.parseMaterial(),
            XMaterial.STONE_BRICK_WALL.parseMaterial()),


    // Fully using streams might be better, but then you are hacking around stuff like Material.PIG_STEP
    // This is simpler and easier to debug than using a stream to do the same thing in less lines
    // We still have to remove the double slabs for legacy versions of the game.
    _SLAB(new CollisionFactory() {
        @Override
        public CollisionBox fetch(ProtocolVersion version, byte data, int x, int y, int z) {
            if ((data & 8) == 0) {
                return new SimpleCollisionBox(0, 0, 0, 1, 0.5, 1);
            }

            return new SimpleCollisionBox(0, 0.5, 0, 1, 1, 1);
        }

        @Override
        public CollisionBox fetch(ProtocolVersion version, BlockData block, int x, int y, int z) {
            Slab slab = (Slab) block;

            if (slab.getType() == Slab.Type.BOTTOM) {
                return new SimpleCollisionBox(0, 0, 0, 1, 0.5, 1);
            } else if (slab.getType() == Slab.Type.TOP) {
                return new SimpleCollisionBox(0, 0.5, 0, 1, 1, 1);
            }

            return new SimpleCollisionBox(0, 0, 0, 1, 1, 1);
        }
    }, Stream.of(XMaterial.OAK_SLAB.parseMaterial(), XMaterial.SPRUCE_SLAB.parseMaterial(), XMaterial.BIRCH_SLAB.parseMaterial(),
            XMaterial.JUNGLE_SLAB.parseMaterial(), XMaterial.ACACIA_SLAB.parseMaterial(), XMaterial.DARK_OAK_SLAB.parseMaterial(),
            XMaterial.CRIMSON_SLAB.parseMaterial(), XMaterial.WARPED_SLAB.parseMaterial(), XMaterial.STONE_SLAB.parseMaterial(),
            XMaterial.SMOOTH_STONE_SLAB.parseMaterial(), XMaterial.SANDSTONE_SLAB.parseMaterial(), XMaterial.CUT_SANDSTONE_SLAB.parseMaterial(),
            XMaterial.PETRIFIED_OAK_SLAB.parseMaterial(), XMaterial.COBBLESTONE_SLAB.parseMaterial(), XMaterial.BRICK_SLAB.parseMaterial(),
            XMaterial.STONE_BRICK_SLAB.parseMaterial(), XMaterial.NETHER_BRICK_SLAB.parseMaterial(), XMaterial.QUARTZ_SLAB.parseMaterial(),
            XMaterial.RED_SANDSTONE_SLAB.parseMaterial(), XMaterial.CUT_RED_SANDSTONE_SLAB.parseMaterial(), XMaterial.PURPUR_SLAB.parseMaterial(),
            XMaterial.PRISMARINE_SLAB.parseMaterial(), XMaterial.PRISMARINE_BRICK_SLAB.parseMaterial(), XMaterial.DARK_PRISMARINE_SLAB.parseMaterial(),
            XMaterial.POLISHED_GRANITE_SLAB.parseMaterial(), XMaterial.SMOOTH_RED_SANDSTONE_SLAB.parseMaterial(), XMaterial.MOSSY_STONE_BRICK_SLAB.parseMaterial(),
            XMaterial.POLISHED_DIORITE_SLAB.parseMaterial(), XMaterial.MOSSY_COBBLESTONE_SLAB.parseMaterial(), XMaterial.END_STONE_BRICK_SLAB.parseMaterial(),
            XMaterial.SMOOTH_SANDSTONE_SLAB.parseMaterial(), XMaterial.SMOOTH_QUARTZ_SLAB.parseMaterial(), XMaterial.GRANITE_SLAB.parseMaterial(),
            XMaterial.ANDESITE_SLAB.parseMaterial(), XMaterial.RED_NETHER_BRICK_SLAB.parseMaterial(), XMaterial.POLISHED_ANDESITE_SLAB.parseMaterial(),
            XMaterial.DIORITE_SLAB.parseMaterial(), XMaterial.BLACKSTONE_SLAB.parseMaterial(), XMaterial.POLISHED_BLACKSTONE_SLAB.parseMaterial(),
            XMaterial.POLISHED_BLACKSTONE_BRICK_SLAB.parseMaterial()).filter(m -> !m.name().contains("DOUBLE")).toArray(Material[]::new)),

    // Note, getting legacy byte seems broken for skulls
    _WALL_SKULL(new CollisionFactory() {
        @Override
        public CollisionBox fetch(ProtocolVersion version, byte data, int x, int y, int z) {
            int rotation = data & 7;
            switch (rotation) {
                // Normal on floor - for any rotation.
                case 1:
                default:
                    return new SimpleCollisionBox(0.25F, 0.0F, 0.25F, 0.75F, 0.5F, 0.75F);
                // Facing north
                case 2:
                    return new SimpleCollisionBox(0.25F, 0.25F, 0.5F, 0.75F, 0.75F, 1.0F);
                // Facing south
                case 3:
                    return new SimpleCollisionBox(0.25F, 0.25F, 0.0F, 0.75F, 0.75F, 0.5F);
                // Facing west
                case 4:
                    return new SimpleCollisionBox(0.5F, 0.25F, 0.25F, 1.0F, 0.75F, 0.75F);
                // Facing east
                case 5:
                    return new SimpleCollisionBox(0.0F, 0.25F, 0.25F, 0.5F, 0.75F, 0.75F);
            }
        }

        // Note that this is for stuff on walls and not regular skull blocks
        @Override
        public CollisionBox fetch(ProtocolVersion version, BlockData block, int x, int y, int z) {
            Directional skullDir = (Directional) block;

            switch (skullDir.getFacing()) {
                // Heads on walls cannot have diagonal rotations
                default:
                    return new SimpleCollisionBox(0.25F, 0.0F, 0.25F, 0.75F, 0.5F, 0.75F);
                case NORTH:
                    return new SimpleCollisionBox(0.25F, 0.25F, 0.5F, 0.75F, 0.75F, 1.0F);
                case SOUTH:
                    return new SimpleCollisionBox(0.25F, 0.25F, 0.0F, 0.75F, 0.75F, 0.5F);
                case WEST:
                    return new SimpleCollisionBox(0.5F, 0.25F, 0.25F, 1.0F, 0.75F, 0.75F);
                case EAST:
                    return new SimpleCollisionBox(0.0F, 0.25F, 0.25F, 0.5F, 0.75F, 0.75F);
            }
        }
    }, XMaterial.SKELETON_WALL_SKULL.parseMaterial(), XMaterial.WITHER_SKELETON_WALL_SKULL.parseMaterial(),
            XMaterial.CREEPER_WALL_HEAD.parseMaterial(), XMaterial.DRAGON_WALL_HEAD.parseMaterial(), // Yes, the dragon head has the same collision box as regular heads
            XMaterial.PLAYER_WALL_HEAD.parseMaterial(), XMaterial.ZOMBIE_WALL_HEAD.parseMaterial()),


    _SKULL(new SimpleCollisionBox(0.25, 0, 0.25, 0.75, 0.5, 0.75), XMaterial.SKELETON_SKULL.parseMaterial(), XMaterial.WITHER_SKELETON_SKULL.parseMaterial(),
            XMaterial.CREEPER_HEAD.parseMaterial(), XMaterial.DRAGON_HEAD.parseMaterial(), // Yes, the dragon head has the same collision box as regular heads
            XMaterial.PLAYER_HEAD.parseMaterial(), XMaterial.ZOMBIE_HEAD.parseMaterial()),


    // TODO: Some of these blocks have a collision box, fix them for the interact check
    _NONE(NoCollisionBox.INSTANCE, XMaterial.TORCH.parseMaterial(), XMaterial.REDSTONE_TORCH.parseMaterial(),
            XMaterial.REDSTONE_WIRE.parseMaterial(), XMaterial.REDSTONE_WALL_TORCH.parseMaterial(), XMaterial.POWERED_RAIL.parseMaterial(), XMaterial.WALL_TORCH.parseMaterial(),
            XMaterial.RAIL.parseMaterial(), XMaterial.ACTIVATOR_RAIL.parseMaterial(), XMaterial.DETECTOR_RAIL.parseMaterial(), XMaterial.AIR.parseMaterial(), XMaterial.TALL_GRASS.parseMaterial(),
            XMaterial.TRIPWIRE.parseMaterial(), XMaterial.TRIPWIRE_HOOK.parseMaterial()),

    _DEFAULT(new SimpleCollisionBox(0, 0, 0, 1, 1, 1),
            XMaterial.STONE.parseMaterial());

    private static final CollisionData[] lookup = new CollisionData[Material.values().length];

    static {
        for (CollisionData data : values()) {
            for (Material mat : data.materials) lookup[mat.ordinal()] = data;
        }
    }

    private final Material[] materials;
    private CollisionBox box;
    private CollisionFactory dynamic;


    CollisionData(CollisionBox box, Material... materials) {
        this.box = box;
        Set<Material> mList = new HashSet<>(Arrays.asList(materials));
        mList.remove(null); // Sets can contain one null
        this.materials = mList.toArray(new Material[0]);
    }

    CollisionData(CollisionFactory dynamic, Material... materials) {
        this.dynamic = dynamic;
        Set<Material> mList = new HashSet<>(Arrays.asList(materials));
        mList.remove(null); // Sets can contain one null
        this.materials = mList.toArray(new Material[0]);
    }

    public static CollisionData getData(Material material) {
        // Material matched = MiscUtils.match(material.toString());
        CollisionData data = lookup[material.ordinal()];
        // _DEFAULT for second thing
        return data != null ? data : _DEFAULT;
    }

    private static Material m(XMaterial xmat) {
        return xmat.parseMaterial();
    }

    public CollisionBox getBox(BlockData block, int x, int y, int z, ProtocolVersion version) {
        if (this.box != null)
            return this.box.copy().offset(x, y, z);
        return new DynamicCollisionBox(dynamic, block, version).offset(x, y, z);
    }
}
