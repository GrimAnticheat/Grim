package ac.grim.grimac.utils.blockdata;

import ac.grim.grimac.utils.blockdata.types.*;
import ac.grim.grimac.utils.blockstate.BaseBlockState;
import ac.grim.grimac.utils.blockstate.FlatBlockState;
import ac.grim.grimac.utils.blockstate.MagicBlockState;
import ac.grim.grimac.utils.nmsImplementations.XMaterial;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.*;
import org.bukkit.block.data.type.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

// Note that the data for these don't reset - default values are unknown - be careful!
public enum WrappedBlockData {

    ANVIL(new WrappedDirectional() {
        public void getWrappedData(FlatBlockState data) {
            Directional facing = (Directional) data.getBlockData();
            setDirection(facing.getFacing());
        }

        public void getWrappedData(MagicBlockState data) {
            switch (data.getBlockData() & 0b01) {
                case (0):
                    setDirection(BlockFace.NORTH);
                    break;
                case (1):
                    setDirection(BlockFace.EAST);
                    break;
            }
        }
    }, XMaterial.ANVIL.parseMaterial(), XMaterial.CHIPPED_ANVIL.parseMaterial(), XMaterial.DAMAGED_ANVIL.parseMaterial()),

    VINE(new WrappedMultipleFacing() {
        public void getWrappedData(FlatBlockState data) {
            setDirections(((MultipleFacing) data.getBlockData()).getFaces());
        }

        public void getWrappedData(MagicBlockState data) {
            switch (data.getBlockData()) {
                case (1):
                    setDirections(BlockFace.SOUTH);
                    break;
                case (2):
                    setDirections(BlockFace.WEST);
                    break;
                case (4):
                    setDirections(BlockFace.NORTH);
                    break;
                case (8):
                    setDirections(BlockFace.EAST);
                    break;
            }
        }
    }, XMaterial.VINE.parseMaterial()),

    CHORUS_PLANT(new WrappedMultipleFacing() {
        public void getWrappedData(FlatBlockState data) {
            setDirections(((MultipleFacing) data.getBlockData()).getFaces());
        }

        public void getWrappedData(MagicBlockState data) {
            // 1.12 doesn't store this blocks' data.
            // It is determined by the state of the world
        }
    }, XMaterial.CHORUS_PLANT.parseMaterial()),

    SLAB(new WrappedSlab() {
        public void getWrappedData(FlatBlockState data) {
            Slab slab = (Slab) data.getBlockData();

            setDouble(slab.getType() == Slab.Type.DOUBLE);

            if (slab.getType() == Slab.Type.BOTTOM) {
                setBottom(true);
            } else if (slab.getType() == Slab.Type.TOP) {
                setBottom(false);
            }
        }

        public void getWrappedData(MagicBlockState data) {
            setDouble(false);
            setBottom((data.getBlockData() & 8) == 0);
        }
        // 1.13 can handle double slabs as it's in the block data
        // 1.12 has double slabs as a separate block, no block data to differentiate it
    }, Arrays.stream(XMaterial.values()).filter(mat -> mat.name().contains("_SLAB"))
            .map(XMaterial::parseMaterial).filter(Objects::nonNull).filter(m -> !m.name().contains("DOUBLE")).toArray(Material[]::new)),

    WALL_SKULL(new WrappedDirectional() {
        public void getWrappedData(FlatBlockState data) {
            setDirection(((Directional) data.getBlockData()).getFacing());
        }

        public void getWrappedData(MagicBlockState data) {
            switch (data.getBlockData() & 7) {
                case 1:
                default: // On the floor
                    setDirection(BlockFace.DOWN);
                    break;
                case 2:
                    setDirection(BlockFace.NORTH);
                    break;
                case 3:
                    setDirection(BlockFace.SOUTH);
                    break;
                case 4:
                    setDirection(BlockFace.WEST);
                    break;
                case 5:
                    setDirection(BlockFace.EAST);
                    break;
            }
        }
    }, XMaterial.SKELETON_WALL_SKULL.parseMaterial(), XMaterial.WITHER_SKELETON_WALL_SKULL.parseMaterial(),
            XMaterial.CREEPER_WALL_HEAD.parseMaterial(), XMaterial.DRAGON_WALL_HEAD.parseMaterial(), // Yes, the dragon head has the same collision box as regular heads
            XMaterial.PLAYER_WALL_HEAD.parseMaterial(), XMaterial.ZOMBIE_WALL_HEAD.parseMaterial()),

    CHEST(new WrappedChest() {

        public void getWrappedData(FlatBlockState data) {
            Chest chest = ((Chest) data.getBlockData());

            setDirection(chest.getFacing());
            setType(chest.getType());
            setTrapped(chest.getMaterial() == Material.TRAPPED_CHEST);
        }

        public void getWrappedData(MagicBlockState data) {
            switch (data.getBlockData() & 7) {
                default:
                case 2:
                    setDirection(BlockFace.NORTH);
                    break;
                case 3:
                    setDirection(BlockFace.SOUTH);
                    break;
                case 4:
                    setDirection(BlockFace.WEST);
                    break;
                case 5:
                    setDirection(BlockFace.EAST);
                    break;
            }

            setTrapped(data.getMaterial() == Material.TRAPPED_CHEST);
        }
    }, XMaterial.CHEST.parseMaterial(), XMaterial.TRAPPED_CHEST.parseMaterial()),


    CAKE(new WrappedCake() {
        public void getWrappedData(FlatBlockState data) {
            Cake cake = (Cake) data.getBlockData();
            setSlices(cake.getBites());
        }

        public void getWrappedData(MagicBlockState data) {
            setSlices(data.getBlockData());
        }
    }, XMaterial.CAKE.parseMaterial()),

    COCOA(new WrappedCocoaBeans() {
        public void getWrappedData(FlatBlockState data) {
            Cocoa cocoa = (Cocoa) data.getBlockData();
            setDirection(cocoa.getFacing());
            setAge(cocoa.getAge());
        }

        public void getWrappedData(MagicBlockState data) {
            switch (data.getBlockData() & (1 << 2) - 1) {
                case 0:
                    setDirection(BlockFace.NORTH);
                    break;
                case 1:
                    setDirection(BlockFace.EAST);
                    break;
                case 2:
                    setDirection(BlockFace.SOUTH);
                    break;
                case 3:
                    setDirection(BlockFace.WEST);
                    break;
            }

            setAge(data.getBlockData() >> 2 & (1 << 2) - 1);
        }
    }, XMaterial.COCOA_BEANS.parseMaterial()),

    GATE(new WrappedFenceGate() {
        public void getWrappedData(FlatBlockState data) {
            Gate gate = (Gate) data.getBlockData();
            setOpen(gate.isOpen());
            setDirection(gate.getFacing());
        }

        public void getWrappedData(MagicBlockState data) {
            setOpen((data.getBlockData() & 0x4) == 0);
            switch (data.getBlockData() & (1 << 2) - 1) {
                case 0:
                    setDirection(BlockFace.NORTH);
                    break;
                case 1:
                    setDirection(BlockFace.EAST);
                    break;
                case 2:
                    setDirection(BlockFace.SOUTH);
                    break;
                case 3:
                    setDirection(BlockFace.WEST);
                    break;
            }
        }
    }, Arrays.stream(XMaterial.values()).filter(mat -> mat.name().contains("FENCE") && mat.name().contains("GATE"))
            .map(XMaterial::parseMaterial)
            .toArray(Material[]::new)),

    // 1.12 doesn't store any data about fences, 1.13+ does
    FENCE(new WrappedMultipleFacing() {
        public void getWrappedData(FlatBlockState data) {
            Fence fence = (Fence) data.getBlockData();
            setDirections(fence.getFaces());
        }

        public void getWrappedData(MagicBlockState data) {

        }
    }, Arrays.stream(XMaterial.values()).filter(mat -> mat.name().contains("FENCE") && !mat.name().contains("GATE"))
            .map(XMaterial::parseMaterial)
            .toArray(Material[]::new)),

    // 1.12 doesn't store any data about panes, 1.13+ does
    GLASS_PANE(new WrappedMultipleFacing() {
        public void getWrappedData(FlatBlockState data) {
            GlassPane pane = (GlassPane) data.getBlockData();
            setDirections(pane.getFaces());
        }

        public void getWrappedData(MagicBlockState data) {

        }
    }, Arrays.stream(XMaterial.values()).filter(mat -> mat.name().contains("GLASS_PANE"))
            .map(XMaterial::parseMaterial)
            .toArray(Material[]::new)),

    // 1.12 doesn't store any data about panes, 1.13+ does
    IRON(new WrappedMultipleFacing() {
        public void getWrappedData(FlatBlockState data) {
            MultipleFacing bar = (MultipleFacing) data.getBlockData();
            setDirections(bar.getFaces());
        }

        public void getWrappedData(MagicBlockState data) {

        }
    }, Arrays.stream(XMaterial.values()).filter(mat -> mat.name().contains("IRON_BARS"))
            .map(XMaterial::parseMaterial)
            .toArray(Material[]::new)),

    // 1.12 doesn't store any data about walls, 1.13+ does
    WALL(new WrappedMultipleFacing() {
        public void getWrappedData(FlatBlockState data) {
            Wall wall = (Wall) data.getBlockData();
            wall.getHeight(BlockFace.NORTH);
        }

        public void getWrappedData(MagicBlockState data) {

        }
    }, Arrays.stream(XMaterial.values()).filter(mat -> mat.name().contains("WALL") && !mat.name().contains("SIGN") && !mat.name().contains("HEAD") && !mat.name().contains("BANNER") &&
            !mat.name().contains("FAN") && !mat.name().contains("SKULL") && !mat.name().contains("TORCH"))
            .map(XMaterial::parseMaterial)
            .toArray(Material[]::new)),

    STAIRS(new WrappedStairs() {
        public void getWrappedData(FlatBlockState data) {
            Stairs stairs = (Stairs) data.getBlockData();
            setUpsideDown(stairs.getHalf() == Bisected.Half.TOP);
            setDirection(stairs.getFacing());
            setShapeOrdinal(stairs.getShape().ordinal());
        }

        public void getWrappedData(MagicBlockState data) {
            setUpsideDown((data.getBlockData() & 0x4) != 0);
            setShapeOrdinal(-1);
            switch (data.getBlockData() & (1 << 2) - 1) {
                case 0:
                    setDirection(BlockFace.EAST);
                    break;
                case 1:
                    setDirection(BlockFace.WEST);
                    break;
                case 2:
                    setDirection(BlockFace.SOUTH);
                    break;
                case 3:
                    setDirection(BlockFace.NORTH);
                    break;
            }
        }
    }, Arrays.stream(XMaterial.values()).filter(mat -> mat.name().endsWith("_STAIRS"))
            .map(XMaterial::parseMaterial)
            .toArray(Material[]::new)),

    SNOW(new WrappedSnow() {
        public void getWrappedData(FlatBlockState data) {
            Snow snow = (Snow) data.getBlockData();
            setLayers(snow.getLayers() - 1);
        }

        public void getWrappedData(MagicBlockState data) {
            setLayers(data.getBlockData());
        }
    }, XMaterial.SNOW.parseMaterial()),

    FRAME(new WrappedFrame() {
        public void getWrappedData(FlatBlockState data) {
            EndPortalFrame frame = (EndPortalFrame) data.getBlockData();
            setHasEye(frame.hasEye());
        }

        public void getWrappedData(MagicBlockState data) {
            setHasEye((data.getBlockData() & 0x04) == 4);
        }
    }, XMaterial.END_PORTAL_FRAME.parseMaterial()),

    END_ROD(new WrappedDirectional() {
        public void getWrappedData(FlatBlockState data) {
            Directional rod = (Directional) data.getBlockData();
            setDirection(rod.getFacing());
        }

        public void getWrappedData(MagicBlockState data) {
            switch (data.getBlockData()) {
                case 0:
                    setDirection(BlockFace.DOWN);
                    break;
                case 1:
                default:
                    setDirection(BlockFace.UP);
                    break;
                case 2:
                    setDirection(BlockFace.NORTH);
                    break;
                case 3:
                    setDirection(BlockFace.SOUTH);
                    break;
                case 4:
                    setDirection(BlockFace.WEST);
                    break;
                case 5:
                    setDirection(BlockFace.EAST);
                    break;
            }
        }
    }, XMaterial.END_ROD.parseMaterial()),

    WALL_SIGN(new WrappedDirectional() {
        public void getWrappedData(FlatBlockState data) {
            Directional rod = (Directional) data.getBlockData();
            setDirection(rod.getFacing());
        }

        public void getWrappedData(MagicBlockState data) {
            switch (data.getBlockData()) {
                case 2:
                    setDirection(BlockFace.NORTH);
                    break;
                case 3:
                    setDirection(BlockFace.SOUTH);
                    break;
                case 4:
                    setDirection(BlockFace.WEST);
                    break;
                case 5:
                    setDirection(BlockFace.EAST);
                    break;
            }
        }
    }, Arrays.stream(Material.values()).filter(mat -> mat.name().contains("WALL_SIGN"))
            .toArray(Material[]::new)),

    BUTTON(new WrappedButton() {
        public void getWrappedData(FlatBlockState data) {
            setDirection(((Directional) data.getBlockData()).getFacing());
            setPowered(((Powerable) data.getBlockData()).isPowered());
        }

        public void getWrappedData(MagicBlockState data) {
            setPowered((data.getBlockData() & 8) == 8);

            switch (data.getBlockData() & 7) {
                case 0:
                    setDirection(BlockFace.DOWN);
                    break;
                case 1:
                    setDirection(BlockFace.EAST);
                    break;
                case 2:
                    setDirection(BlockFace.WEST);
                    break;
                case 3:
                    setDirection(BlockFace.SOUTH);
                    break;
                case 4:
                    setDirection(BlockFace.NORTH);
                    break;
                case 5:
                    setDirection(BlockFace.UP);
                    break;
            }
        }
    }, Arrays.stream(Material.values()).filter(mat -> mat.name().contains("BUTTON")).toArray(Material[]::new)),

    LADDER(new WrappedDirectional() {
        public void getWrappedData(FlatBlockState data) {
            Directional ladder = (Directional) data.getBlockData();
            setDirection(ladder.getFacing());
        }

        public void getWrappedData(MagicBlockState data) {
            switch (data.getBlockData()) {
                case 2:
                    setDirection(BlockFace.NORTH);
                    break;
                case 3:
                    setDirection(BlockFace.SOUTH);
                    break;
                case 4:
                    setDirection(BlockFace.WEST);
                    break;
                case 5:
                    setDirection(BlockFace.EAST);
                    break;
            }
        }
    }, XMaterial.LADDER.parseMaterial()),

    LEVER(new WrappedDirectional() {
        public void getWrappedData(FlatBlockState data) {
            setDirection(((Directional) data.getBlockData()).getFacing());
        }

        public void getWrappedData(MagicBlockState data) {
            switch (data.getBlockData() & 7) {
                case 0:
                case 7:
                    setDirection(BlockFace.DOWN);
                    break;
                case 1:
                    setDirection(BlockFace.EAST);
                    break;
                case 2:
                    setDirection(BlockFace.WEST);
                    break;
                case 3:
                    setDirection(BlockFace.SOUTH);
                    break;
                case 4:
                    setDirection(BlockFace.NORTH);
                    break;
                case 5:
                case 6:
                    setDirection(BlockFace.UP);
                    break;
            }
        }
    }, XMaterial.LEVER.parseMaterial()),

    WALL_TORCH(new WrappedDirectional() {
        public void getWrappedData(FlatBlockState data) {
            setDirection(((Directional) data.getBlockData()).getFacing());
        }

        public void getWrappedData(MagicBlockState data) {
            switch (data.getBlockData() & 7) {
                case 1:
                    setDirection(BlockFace.EAST);
                    break;
                case 2:
                    setDirection(BlockFace.WEST);
                    break;
                case 3:
                    setDirection(BlockFace.SOUTH);
                    break;
                case 4:
                    setDirection(BlockFace.NORTH);
                    break;
                case 5:
                    setDirection(BlockFace.UP);
                    break;
            }
        }
    }, XMaterial.WALL_TORCH.parseMaterial(), XMaterial.REDSTONE_WALL_TORCH.parseMaterial()),

    PISTON_BASE(new WrappedPistonBase() {
        public void getWrappedData(FlatBlockState data) {
            Piston piston = (Piston) data.getBlockData();
            setPowered(piston.isExtended());
            setDirection(piston.getFacing());
        }

        public void getWrappedData(MagicBlockState data) {
            int magic = data.getData();

            setPowered((magic & 8) != 0);

            if (isPowered()) {
                switch (magic & 7) {
                    case 0:
                        setDirection(BlockFace.DOWN);
                        break;
                    case 1:
                        setDirection(BlockFace.UP);
                        break;
                    case 2:
                        setDirection(BlockFace.NORTH);
                        break;
                    case 3:
                        setDirection(BlockFace.SOUTH);
                        break;
                    case 4:
                        setDirection(BlockFace.WEST);
                        break;
                    case 5:
                        setDirection(BlockFace.EAST);
                        break;
                }
            }
        }
    }, XMaterial.PISTON.parseMaterial(), XMaterial.STICKY_PISTON.parseMaterial()),

    PISTON_EXTENSION(new WrappedPiston() {
        public void getWrappedData(FlatBlockState data) {
            PistonHead head = (PistonHead) data.getBlockData();
            setDirection(head.getFacing());
            setShort(head.isShort());
        }

        public void getWrappedData(MagicBlockState data) {
            // Short pistons are pistons that are currently extending or retracting
            // There is no block data to differentiate these in 1.12
            // In testing, I can only get
            setShort(false);
            switch (data.getBlockData() & 7) {
                case 0:
                    setDirection(BlockFace.DOWN);
                    break;
                case 1:
                    setDirection(BlockFace.UP);
                    break;
                case 2:
                    setDirection(BlockFace.NORTH);
                    break;
                case 3:
                    setDirection(BlockFace.SOUTH);
                    break;
                case 4:
                    setDirection(BlockFace.WEST);
                    break;
                case 5:
                    setDirection(BlockFace.EAST);
                    break;
            }
        }
    }, XMaterial.PISTON_HEAD.parseMaterial()),

    RAILS(new WrappedRails() {
        public void getWrappedData(FlatBlockState data) {
            Rail rail = (Rail) data.getBlockData();

            setAscending(rail.getShape() == Rail.Shape.ASCENDING_EAST || rail.getShape() == Rail.Shape.ASCENDING_WEST
                    || rail.getShape() == Rail.Shape.ASCENDING_NORTH || rail.getShape() == Rail.Shape.ASCENDING_SOUTH);
        }

        public void getWrappedData(MagicBlockState data) {
            int magic = data.getBlockData();
            // Magic values 2 to 5 are ascending
            setAscending(magic > 1 && magic < 6);
        }
    }, Arrays.stream(Material.values()).filter(mat -> mat.name().contains("RAIL")).toArray(Material[]::new)),

    DOOR(new WrappedDoor() {
        public void getWrappedData(FlatBlockState data) {
            Door door = (Door) data.getBlockData();
            setDirection(door.getFacing());
            setOpen(door.isOpen());
            setRightHinge(door.getHinge() == Door.Hinge.RIGHT);
            setBottom(door.getHalf() == Bisected.Half.BOTTOM);
        }

        public void getWrappedData(MagicBlockState data) {
            int magic = data.getData();

            setBottom((magic & 0b1000) == 0);

            if (isBottom()) {
                setOpen((magic & 0b10) != 0);

                switch (magic & 0b11) {
                    case 0:
                        setDirection(BlockFace.EAST);
                        break;
                    case 1:
                        setDirection(BlockFace.SOUTH);
                        break;
                    case 2:
                        setDirection(BlockFace.WEST);
                        break;
                    case 3:
                        setDirection(BlockFace.NORTH);
                        break;
                }
            } else {
                setRightHinge((magic & 0b1) == 0);
            }

            setOpen((magic & 0b100) != 0);
        }
    }, Arrays.stream(XMaterial.values()).filter(mat -> mat.name().contains("_DOOR"))
            .map(XMaterial::parseMaterial).toArray(Material[]::new)),

    TRAPDOOR(new WrappedTrapdoor() {
        public void getWrappedData(FlatBlockState data) {
            TrapDoor trapDoor = (TrapDoor) data.getBlockData();
            setOpen(trapDoor.isOpen());
            setBottom(trapDoor.getHalf() == Bisected.Half.BOTTOM);
            setDirection(trapDoor.getFacing());
        }

        public void getWrappedData(MagicBlockState data) {
            int magic = data.getBlockData();
            setOpen((magic & 0x4) == 4);
            setBottom((magic & 0x8) == 0);

            // Magic values 2 to 5 are ascending
            switch (magic & 7) {
                case 0:
                    setDirection(BlockFace.SOUTH);
                case 1:
                    setDirection(BlockFace.NORTH);
                case 2:
                    setDirection(BlockFace.EAST);
                case 3:
                    setDirection(BlockFace.WEST);
            }
        }
    }, Arrays.stream(Material.values())
            .filter(mat -> mat.name().contains("TRAP_DOOR") || mat.name().contains("TRAPDOOR")).toArray(Material[]::new)),

    FLAT_ONLY_BLOCK(new WrappedFlatBlock() {
        public void getWrappedData(FlatBlockState data) {
            setBlockData(data.getBlockData());
        }
    }, XMaterial.BELL.parseMaterial(), XMaterial.LANTERN.parseMaterial(), XMaterial.GRINDSTONE.parseMaterial(),
            XMaterial.CHAIN.parseMaterial(), XMaterial.SWEET_BERRIES.parseMaterial(), XMaterial.SEA_PICKLE.parseMaterial(),
            XMaterial.TURTLE_EGG.parseMaterial()),

    NO_DATA(new WrappedBlockDataValue(), XMaterial.AIR.parseMaterial());

    private static final WrappedBlockData[] lookup = new WrappedBlockData[Material.values().length];

    static {
        for (WrappedBlockData data : values()) {
            for (Material mat : data.materials) lookup[mat.ordinal()] = data;
        }
    }

    private final WrappedBlockDataValue data;
    private final Material[] materials;

    WrappedBlockData(WrappedBlockDataValue data, Material... materials) {
        this.data = data;
        Set<Material> mList = new HashSet<>(Arrays.asList(materials));
        mList.remove(null); // Sets can contain one null
        this.materials = mList.toArray(new Material[0]);
    }

    public static WrappedBlockDataValue getMaterialData(BaseBlockState state) {
        WrappedBlockData data = lookup[state.getMaterial().ordinal()];

        if (data != null) {
            try {
                // We need to create a new instance because the anticheat is multithreaded
                WrappedBlockDataValue newData = data.data.getClass().newInstance();
                newData.getData(state);
                return newData;
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        return NO_DATA.data;
    }
}