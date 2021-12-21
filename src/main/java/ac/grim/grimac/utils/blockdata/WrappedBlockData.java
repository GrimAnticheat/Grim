package ac.grim.grimac.utils.blockdata;

import ac.grim.grimac.utils.blockdata.types.*;
import ac.grim.grimac.utils.blockstate.BaseBlockState;
import ac.grim.grimac.utils.blockstate.FlatBlockState;
import ac.grim.grimac.utils.blockstate.MagicBlockState;
import ac.grim.grimac.utils.nmsutil.Materials;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.*;
import org.bukkit.block.data.type.*;
import org.bukkit.material.PressureSensor;
import org.bukkit.material.Redstone;

import java.util.Arrays;
import java.util.HashSet;
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
    }, ItemTypes.ANVIL, ItemTypes.CHIPPED_ANVIL, ItemTypes.DAMAGED_ANVIL),

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
    }, ItemTypes.VINE),

    HOPPER(new WrappedDirectional() {
        public void getWrappedData(FlatBlockState data) {
            setDirection(((Directional) data.getBlockData()).getFacing());
        }

        // 0x8 is activated/disabled
        public void getWrappedData(MagicBlockState data) {
            switch (data.getBlockData() & 7) {
                case 0:
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
    }, ItemTypes.HOPPER),

    CHORUS_PLANT(new WrappedMultipleFacing() {
        public void getWrappedData(FlatBlockState data) {
            setDirections(((MultipleFacing) data.getBlockData()).getFaces());
        }

        public void getWrappedData(MagicBlockState data) {
            // 1.12 doesn't store this blocks' data.
            // It is determined by the state of the world
        }
    }, ItemTypes.CHORUS_PLANT),

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
    }, Arrays.stream(Material.values()).filter(mat -> (mat.name().contains("_SLAB") || mat.name().contains("STEP"))
            && !mat.name().contains("DOUBLE")).toArray(Material[]::new)),

    BED(new WrappedDirectional() {
        public void getWrappedData(FlatBlockState data) {
            Bed bed = (Bed) data.getBlockData();
            setDirection(bed.getPart() == Bed.Part.HEAD ? bed.getFacing() : bed.getFacing().getOppositeFace());
        }

        public void getWrappedData(MagicBlockState data) {
            boolean isFoot = (data.getBlockData() & 0x8) == 0;
            switch (data.getBlockData() & 3) {
                case 0:
                    setDirection(isFoot ? BlockFace.NORTH : BlockFace.SOUTH);
                    break;
                case 1:
                    setDirection(isFoot ? BlockFace.EAST : BlockFace.WEST);
                    break;
                case 2:
                    setDirection(isFoot ? BlockFace.SOUTH : BlockFace.NORTH);
                    break;
                case 3:
                    setDirection(isFoot ? BlockFace.WEST : BlockFace.EAST);
                    break;
            }
        }
    }, Arrays.stream(Material.values()).filter(mat -> mat.name().contains("BED") && !mat.name().contains("ROCK"))
            .toArray(Material[]::new)),

    WALL_SKULL(new WrappedDirectional() {
        public void getWrappedData(FlatBlockState data) {
            // Heads on the floor are not directional
            if (!(data.getBlockData() instanceof Directional)) {
                setDirection(BlockFace.DOWN);
                return;
            }

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
    }, Arrays.stream(Material.values()).filter(mat -> (mat.name().contains("HEAD") || mat.name().contains("SKULL")) && !mat.name().contains("PISTON")).toArray(Material[]::new)),

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
    }, ItemTypes.CHEST, ItemTypes.TRAPPED_CHEST),


    CAKE(new WrappedCake() {
        public void getWrappedData(FlatBlockState data) {
            Cake cake = (Cake) data.getBlockData();
            setSlices(cake.getBites());
        }

        public void getWrappedData(MagicBlockState data) {
            setSlices(data.getBlockData());
        }
    }, ItemTypes.CAKE),

    COCOA(new WrappedCocoaBeans() {
        public void getWrappedData(FlatBlockState data) {
            Cocoa cocoa = (Cocoa) data.getBlockData();
            setDirection(cocoa.getFacing());
            setAge(cocoa.getAge());
        }

        public void getWrappedData(MagicBlockState data) {
            switch (data.getBlockData() & (1 << 2) - 1) {
                case 0:
                    setDirection(BlockFace.SOUTH);
                    break;
                case 1:
                    setDirection(BlockFace.WEST);
                    break;
                case 2:
                    setDirection(BlockFace.NORTH);
                    break;
                case 3:
                    setDirection(BlockFace.EAST);
                    break;
            }

            setAge(data.getBlockData() >> 2 & (1 << 2) - 1);
        }
    }, ItemTypes.COCOA),

    GATE(new WrappedFenceGate() {
        public void getWrappedData(FlatBlockState data) {
            Gate gate = (Gate) data.getBlockData();
            setOpen(gate.isOpen());
            setDirection(gate.getFacing());
        }

        public void getWrappedData(MagicBlockState data) {
            setOpen((data.getBlockData() & 0x4) != 0);
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
    }, Arrays.stream(Material.values()).filter(mat -> mat.name().contains("FENCE") && mat.name().contains("GATE"))
            .toArray(Material[]::new)),

    // 1.12 doesn't store any data about fences, 1.13+ does
    FENCE(new WrappedMultipleFacing() {
        public void getWrappedData(FlatBlockState data) {
            Fence fence = (Fence) data.getBlockData();
            setDirections(fence.getFaces());
        }

        public void getWrappedData(MagicBlockState data) {

        }
    }, Arrays.stream(Material.values()).filter(mat -> mat.name().contains("FENCE") && !mat.name().contains("GATE") && !mat.name().contains("IRON_FENCE"))
            .toArray(Material[]::new)),

    // 1.12 doesn't store any data about panes, 1.13+ does
    GLASS_PANE(new WrappedMultipleFacing() {
        public void getWrappedData(FlatBlockState data) {
            MultipleFacing pane = (MultipleFacing) data.getBlockData();
            setDirections(pane.getFaces());
        }

        public void getWrappedData(MagicBlockState data) {

        }
    }, Arrays.stream(Material.values()).filter(mat -> mat.name().contains("GLASS_PANE") || mat.name().contains("IRON_BARS") || mat.name().contains("IRON_FENCE") || mat.name().contains("THIN_GLASS"))
            .toArray(Material[]::new)),

    // 1.12 doesn't store any data about walls, 1.13+ does
    // 1.16 has the Wall data type, 1.13-1.15 uses MultipleFacing
    WALL(new WrappedMultipleFacing() {
        public void getWrappedData(FlatBlockState data) {
            if (ItemTypes.supports(16)) {
                Wall wall = (Wall) data.getBlockData();
                Set<BlockFace> directions = new HashSet<>();

                if (wall.getHeight(BlockFace.NORTH) != Wall.Height.NONE)
                    directions.add(BlockFace.NORTH);
                if (wall.getHeight(BlockFace.EAST) != Wall.Height.NONE)
                    directions.add(BlockFace.EAST);
                if (wall.getHeight(BlockFace.SOUTH) != Wall.Height.NONE)
                    directions.add(BlockFace.SOUTH);
                if (wall.getHeight(BlockFace.WEST) != Wall.Height.NONE)
                    directions.add(BlockFace.WEST);
                if (wall.isUp())
                    directions.add(BlockFace.UP);

                setDirections(directions);
            } else {
                MultipleFacing facing = (MultipleFacing) data.getBlockData();
                setDirections(facing.getFaces());
            }
        }

        public void getWrappedData(MagicBlockState data) {

        }
    }, Arrays.stream(Material.values()).filter(mat -> mat.name().contains("WALL") && !mat.name().contains("SIGN") && !mat.name().contains("HEAD") && !mat.name().contains("BANNER") &&
                    !mat.name().contains("FAN") && !mat.name().contains("SKULL") && !mat.name().contains("TORCH"))
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
    }, Arrays.stream(Material.values()).filter(mat -> mat.name().endsWith("_STAIRS"))
            .toArray(Material[]::new)),

    SNOW(new WrappedSnow() {
        public void getWrappedData(FlatBlockState data) {
            Snow snow = (Snow) data.getBlockData();
            setLayers(snow.getLayers() - 1);
        }

        public void getWrappedData(MagicBlockState data) {
            setLayers(data.getBlockData());
        }
    }, ItemTypes.SNOW),

    AGEABLE(new WrappedAgeable() {
        public void getWrappedData(FlatBlockState data) {
            Ageable ageable = (Ageable) data.getBlockData();
            setAge(ageable.getAge());
        }

        public void getWrappedData(MagicBlockState data) {
            setAge(data.getBlockData());
        }
    }, ItemTypes.BEETROOT, ItemTypes.CARROT, ItemTypes.POTATO,
            ItemTypes.WHEAT, ItemTypes.NETHER_WART,
            ItemTypes.PUMPKIN_STEM, ItemTypes.MELON_STEM),

    FRAME(new WrappedFrame() {
        public void getWrappedData(FlatBlockState data) {
            EndPortalFrame frame = (EndPortalFrame) data.getBlockData();
            setHasEye(frame.hasEye());
        }

        public void getWrappedData(MagicBlockState data) {
            setHasEye((data.getBlockData() & 0x04) == 4);
        }
    }, ItemTypes.END_PORTAL_FRAME),

    ROD(new WrappedDirectional() {
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
    }, ItemTypes.END_ROD, ItemTypes.LIGHTNING_ROD),


    SHULKER_BOX(new WrappedDirectional() {
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
    }, Arrays.stream(Material.values()).filter(mat -> mat.name().contains("SHULKER_BOX"))
            .toArray(Material[]::new)),

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

    BUTTON(new WrappedDirectionalPower() {
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
    }, ItemTypes.LADDER),

    LEVER(new WrappedDirectionalPower() {
        public void getWrappedData(FlatBlockState data) {
            setDirection(((Directional) data.getBlockData()).getFacing());
            setPowered(((Redstone) data.getBlockData()).isPowered());
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
            setPowered((data.getBlockData() & 0x8) == 0x8);
        }
    }, ItemTypes.LEVER),

    TRIPWIRE(new WrappedTripwire() {
        public void getWrappedData(FlatBlockState data) {
            setAttached(((TripwireHook) data.getBlockData()).isAttached());
        }

        public void getWrappedData(MagicBlockState data) {
            setAttached((data.getBlockData() & 0x4) == 0x4);
        }
    }, ItemTypes.TRIPWIRE),

    TRIPWIRE_HOOK(new WrappedDirectionalPower() {
        public void getWrappedData(FlatBlockState data) {
            setDirection(((Directional) data.getBlockData()).getFacing());
            setPowered(((Redstone) data.getBlockData()).isPowered());
        }

        public void getWrappedData(MagicBlockState data) {
            switch (data.getBlockData() & 3) {
                case 0:
                    setDirection(BlockFace.SOUTH);
                    break;
                case 1:
                    setDirection(BlockFace.WEST);
                    break;
                case 2:
                    setDirection(BlockFace.NORTH);
                    break;
                case 3:
                    setDirection(BlockFace.EAST);
                    break;
            }
            setPowered((data.getBlockData() & 0x8) == 0x8);
        }
    }, ItemTypes.TRIPWIRE_HOOK),

    OBSERVER(new WrappedDirectionalPower() {
        public void getWrappedData(FlatBlockState data) {
            setDirection(((Directional) data.getBlockData()).getFacing());
            setPowered(((Redstone) data.getBlockData()).isPowered());
        }

        public void getWrappedData(MagicBlockState data) {
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
            setPowered((data.getBlockData() & 0x8) == 0x8);
        }
    }, ItemTypes.OBSERVER),

    REDSTONE_WIRE(new WrappedMultipleFacingPower() {
        public void getWrappedData(FlatBlockState data) {
            RedstoneWire redstone = (RedstoneWire) data.getBlockData();

            HashSet<BlockFace> directions = new HashSet<>();

            for (BlockFace face : BlockFace.values()) {
                if (face == BlockFace.NORTH || face == BlockFace.EAST || face == BlockFace.SOUTH || face == BlockFace.WEST) {
                    if (redstone.getFace(face) != RedstoneWire.Connection.NONE) {
                        directions.add(face);
                    }
                }
            }

            setDirections(directions);
            setPower(redstone.getPower());
        }

        // There aren't connections in block data on 1.12!
        public void getWrappedData(MagicBlockState data) {
            setPower(data.getBlockData());
        }
    }, ItemTypes.REDSTONE_WIRE),

    WALL_TORCH(new WrappedWallTorchDirectionalPower() {
        public void getWrappedData(FlatBlockState data) {
            setDirection(((Directional) data.getBlockData()).getFacing());
            if (data.getBlockData() instanceof Lightable) {
                setPowered(((Lightable) data.getBlockData()).isLit());
            }
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
            setPowered((data.getBlockData() & 0x8) == 0x8);
        }
    }, ItemTypes.WALL_TORCH, ItemTypes.REDSTONE_WALL_TORCH),

    REDSTONE_TORCH(new WrappedRedstoneTorch() {
        public void getWrappedData(FlatBlockState data) {
            setPower(((Lightable) data.getBlockData()).isLit() ? 15 : 0);
        }

        public void getWrappedData(MagicBlockState data) {
            // Stored in name again because mojang -_-
            setPower(data.getMaterial().name().equalsIgnoreCase("REDSTONE_TORCH_ON") ? 15 : 0);
        }
    }, ItemTypes.REDSTONE_TORCH,
            Materials.matchLegacy("REDSTONE_TORCH_OFF"), Materials.matchLegacy("REDSTONE_TORCH_ON")),

    PISTON_BASE(new WrappedPistonBase() {
        public void getWrappedData(FlatBlockState data) {
            Piston piston = (Piston) data.getBlockData();
            setPowered(piston.isExtended());
            setDirection(piston.getFacing());
        }

        public void getWrappedData(MagicBlockState data) {
            int magic = data.getBlockData();

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
    }, ItemTypes.PISTON, ItemTypes.STICKY_PISTON),

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
    }, ItemTypes.PISTON_HEAD),

    RAILS(new WrappedRails() {
        public void getWrappedData(FlatBlockState data) {
            Rail rail = (Rail) data.getBlockData();

            setAscending(rail.getShape() == Rail.Shape.ASCENDING_EAST || rail.getShape() == Rail.Shape.ASCENDING_WEST
                    || rail.getShape() == Rail.Shape.ASCENDING_NORTH || rail.getShape() == Rail.Shape.ASCENDING_SOUTH);

            if (data.getMaterial() == Material.DETECTOR_RAIL) {
                setPower(((RedstoneRail) rail).isPowered() ? 15 : 0);
            }
        }

        public void getWrappedData(MagicBlockState data) {
            int magic = data.getBlockData();
            // Magic values 2 to 5 are ascending
            setAscending(magic > 1 && magic < 6);
            setPower((magic & 0x8) == 0x8 ? 15 : 0);
        }
    }, Arrays.stream(Material.values()).filter(mat -> mat.name().contains("RAIL")).toArray(Material[]::new)),

    // Block power is wrong for weighted pressure plates, but grim only needs to know if there is block power
    PRESSURE_PLATE(new WrappedPower() {
        public void getWrappedData(FlatBlockState data) {
            PressureSensor sensor = (PressureSensor) data.getBlockData();
            setPower(sensor.isPressed() ? 15 : 0);
        }

        public void getWrappedData(MagicBlockState data) {
            int magic = data.getBlockData();
            setPower(magic != 0 ? 15 : 0);
        }
    }, Arrays.stream(Material.values()).filter(mat -> mat.name().contains("PLATE")).toArray(Material[]::new)),

    DAYLIGHT_SENSOR(new WrappedPower() {
        public void getWrappedData(FlatBlockState data) {
            DaylightDetector detector = (DaylightDetector) data.getBlockData();
            setPower(detector.getPower());
        }

        public void getWrappedData(MagicBlockState data) {
            int magic = data.getBlockData();
            setPower(magic);
        }
    }, Arrays.stream(Material.values()).filter(mat -> mat.name().contains("DAYLIGHT")).toArray(Material[]::new)),

    REPEATER(new WrappedDirectionalPower() {
        public void getWrappedData(FlatBlockState data) {
            setDirection(((Directional) data.getBlockData()).getFacing());
            setPowered(((Powerable) data.getBlockData()).isPowered());
        }

        public void getWrappedData(MagicBlockState data) {
            // 1.12 is limited by states and therefore use different materials for power state
            setPowered(data.getMaterial().name().endsWith("ON"));

            switch (data.getBlockData() & 3) {
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
    }, Materials.matchLegacy("LEGACY_DIODE_BLOCK_OFF"), Materials.matchLegacy("LEGACY_DIODE_BLOCK_ON"),
            ItemTypes.REPEATER),

    DOOR(new WrappedDoor() {
        public void getWrappedData(FlatBlockState data) {
            Door door = (Door) data.getBlockData();
            setDirection(door.getFacing());
            setOpen(door.isOpen());
            setRightHinge(door.getHinge() == Door.Hinge.RIGHT);
            setBottom(door.getHalf() == Bisected.Half.BOTTOM);
        }

        public void getWrappedData(MagicBlockState data) {
            int magic = data.getBlockData();

            setBottom((magic & 0x8) == 0);

            if (isBottom()) {
                setOpen((magic & 0x4) != 0);

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
                setRightHinge((magic & 0x1) != 0);
            }
        }
    }, Arrays.stream(Material.values()).filter(mat -> mat.name().contains("_DOOR"))
            .toArray(Material[]::new)),

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

            // Note that 1.12 and 1.13 swap direction, we use 1.13 values and account for that here
            switch (magic & 0b11) {
                case 0:
                    setDirection(BlockFace.NORTH);
                    break;
                case 1:
                    setDirection(BlockFace.SOUTH);
                    break;
                case 2:
                    setDirection(BlockFace.WEST);
                    break;
                case 3:
                    setDirection(BlockFace.EAST);
                    break;
            }
        }
    }, Arrays.stream(Material.values()).filter(mat -> mat.name().contains("TRAP_DOOR") || mat.name().contains("TRAPDOOR")).toArray(Material[]::new)),

    CANDLE(new WrappedFlatBlock() {
        public void getWrappedData(FlatBlockState data) {
            setBlockData(data.getBlockData());
        }
    }, Arrays.stream(Material.values()).filter(mat -> mat.name().endsWith("CANDLE")).toArray(Material[]::new)),

    CANDLE_CAKE(new WrappedFlatBlock() {
        public void getWrappedData(FlatBlockState data) {
            setBlockData(data.getBlockData());
        }
    }, Arrays.stream(Material.values()).filter(mat -> mat.name().endsWith("CANDLE_CAKE")).toArray(Material[]::new)),


    FLAT_ONLY_BLOCK(new WrappedFlatBlock() {
        public void getWrappedData(FlatBlockState data) {
            setBlockData(data.getBlockData());
        }
    }, ItemTypes.BELL, ItemTypes.LANTERN, ItemTypes.SOUL_LANTERN,
            ItemTypes.GRINDSTONE, ItemTypes.CHAIN,
            ItemTypes.SWEET_BERRIES, ItemTypes.SEA_PICKLE,
            ItemTypes.CAMPFIRE, ItemTypes.SOUL_CAMPFIRE,
            ItemTypes.TURTLE_EGG, ItemTypes.SCAFFOLDING,
            ItemTypes.SCULK_SENSOR, ItemTypes.BIG_DRIPLEAF,
            ItemTypes.POINTED_DRIPSTONE, ItemTypes.AMETHYST_CLUSTER,
            ItemTypes.POWDER_SNOW, ItemTypes.SMALL_AMETHYST_BUD,
            ItemTypes.MEDIUM_AMETHYST_BUD, ItemTypes.LARGE_AMETHYST_BUD,
            ItemTypes.CANDLE, ItemTypes.LAVA,
            ItemTypes.ATTACHED_MELON_STEM, ItemTypes.ATTACHED_PUMPKIN_STEM), // Lava is only solid on 1.16+


    NO_DATA(new WrappedBlockDataValue(), ItemTypes.AIR);

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