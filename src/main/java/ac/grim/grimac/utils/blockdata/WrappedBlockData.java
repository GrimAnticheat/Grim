package ac.grim.grimac.utils.blockdata;

import ac.grim.grimac.utils.blockstate.FlatBlockState;
import ac.grim.grimac.utils.blockstate.MagicBlockState;
import ac.grim.grimac.utils.nmsImplementations.XMaterial;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.block.data.Powerable;
import org.bukkit.block.data.Rail;
import org.bukkit.block.data.type.*;

import java.util.Arrays;
import java.util.Objects;

public enum WrappedBlockData {

    ANVIL(new WrappedDirectional() {
        public void getWrappedData(FlatBlockState data) {
            Directional facing = (Directional) data;
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
            directions = ((MultipleFacing) data.getBlockData()).getFaces();
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

    SLAB(new WrappedSlab() {
        public void getWrappedData(FlatBlockState data) {
            Slab slab = (Slab) data.getBlockData();

            if (slab.getType() == Slab.Type.BOTTOM) {
                isBottom = true;
            } else if (slab.getType() == Slab.Type.TOP) {
                isBottom = false;
            } else {
                isDouble = true;
            }
        }

        public void getWrappedData(MagicBlockState data) {
            isBottom = (data.getBlockData() & 8) == 0;
        }
        // 1.13 can handle double slabs as it's in the block data
        // 1.12 has double slabs as a separate block, no block data to differentiate it
    }, Arrays.stream(XMaterial.values()).filter(mat -> mat.name().contains("_SLAB"))
            .map(XMaterial::parseMaterial).filter(Objects::nonNull).filter(m -> !m.name().contains("DOUBLE")).toArray(Material[]::new)),

    WALL_SKULL(new WrappedDirectional() {
        public void getWrappedData(FlatBlockState data) {
            direction = ((Directional) data.getBlockData()).getFacing();
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

    CAKE(new WrappedCake() {
        public void getWrappedData(FlatBlockState data) {
            Cake cake = (Cake) data;
            slices = cake.getBites();
        }

        public void getWrappedData(MagicBlockState data) {
            slices = data.getBlockData();
        }
    }, XMaterial.CAKE.parseMaterial()),

    COCOA(new WrappedCocoaBeans() {
        public void getWrappedData(FlatBlockState data) {
            Cocoa cocoa = (Cocoa) data;
            setDirection(cocoa.getFacing());
            age = cocoa.getAge();
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

            age = (data.getBlockData() >> 2 & (1 << 2) - 1);
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
            Directional rod = (Directional) data;
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
            Directional rod = (Directional) data;
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
            setDirection(((Directional) data).getFacing());
            setPowered(((Powerable) data).isPowered());
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

    LEVER(new WrappedDirectional() {
        public void getWrappedData(FlatBlockState data) {
            setDirection(((Directional) data).getFacing());
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
            setDirection(((Directional) data).getFacing());
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

    TRAPDOOR(new WrappedTrapdoor() {
        public void getWrappedData(FlatBlockState data) {
            TrapDoor trapDoor = (TrapDoor) data;
            setOpen(trapDoor.isOpen());
            setDirection(trapDoor.getFacing());
        }

        public void getWrappedData(MagicBlockState data) {
            int magic = data.getBlockData();
            setOpen((magic & 0x4) == 4);

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
            this.blockData = data.getBlockData();
        }
    }, XMaterial.BELL.parseMaterial(), XMaterial.LANTERN.parseMaterial(), XMaterial.LECTERN.parseMaterial(),
            XMaterial.GRINDSTONE.parseMaterial(), XMaterial.CHAIN.parseMaterial(), XMaterial.SWEET_BERRIES.parseMaterial()),

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
        this.materials = materials;
    }

    public static WrappedBlockDataValue getMaterialData(Material material) {
        WrappedBlockData data = lookup[material.ordinal()];

        return data != null ? data.data : NO_DATA.data;
    }
}