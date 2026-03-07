package ac.grim.legacyac.util.collision;

import ac.grim.legacyac.world.LegacyBlockState;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.block.Block;

public final class LegacyBlockBoxResolver {
    private LegacyBlockBoxResolver() {
    }

    public interface BlockAccess {
        LegacyBlockState getBlockState(int x, int y, int z);
    }

    public static final class Box {
        private final double minX;
        private final double minY;
        private final double minZ;
        private final double maxX;
        private final double maxY;
        private final double maxZ;

        public Box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
        }

        public double getMinX() { return minX; }
        public double getMinY() { return minY; }
        public double getMinZ() { return minZ; }
        public double getMaxX() { return maxX; }
        public double getMaxY() { return maxY; }
        public double getMaxZ() { return maxZ; }

        public double volume() {
            return Math.max(0.0D, maxX - minX) * Math.max(0.0D, maxY - minY) * Math.max(0.0D, maxZ - minZ);
        }

        public double overlapVolume(Box other) {
            double ox = Math.min(maxX, other.maxX) - Math.max(minX, other.minX);
            double oy = Math.min(maxY, other.maxY) - Math.max(minY, other.minY);
            double oz = Math.min(maxZ, other.maxZ) - Math.max(minZ, other.minZ);
            if (ox <= 0.0D || oy <= 0.0D || oz <= 0.0D) {
                return 0.0D;
            }
            return ox * oy * oz;
        }
    }

    public static List<Box> resolve(Block block) {
        if (block == null) {
            return new ArrayList<Box>();
        }
        return resolve(new LegacyBlockState(block.getType(), block.getData()),
                block.getX(), block.getY(), block.getZ(), new BlockAccess() {
                    @Override
                    public LegacyBlockState getBlockState(int x, int y, int z) {
                        return LegacyBlockState.fromBlock(block.getWorld().getBlockAt(x, y, z));
                    }
                });
    }

    public static List<Box> resolve(LegacyBlockState state, int bx, int by, int bz, BlockAccess access) {
        List<Box> boxes = new ArrayList<Box>();
        Material type = state == null ? Material.AIR : state.getType();
        byte data = state == null ? 0 : state.getData();
        if (type == Material.AIR || type == Material.WATER || type == Material.STATIONARY_WATER
                || type == Material.LAVA || type == Material.STATIONARY_LAVA || type == Material.WEB) {
            return boxes;
        }

        if (isPane(type)) {
            boolean north = connectsTo(access, bx, by, bz - 1, true);
            boolean south = connectsTo(access, bx, by, bz + 1, true);
            boolean west = connectsTo(access, bx - 1, by, bz, true);
            boolean east = connectsTo(access, bx + 1, by, bz, true);
            addPaneOrFencePostAndArms(boxes, bx, by, bz, north, south, west, east, 1.0D, 0.125D);
            return boxes;
        }

        if (isFence(type)) {
            boolean north = connectsTo(access, bx, by, bz - 1, false);
            boolean south = connectsTo(access, bx, by, bz + 1, false);
            boolean west = connectsTo(access, bx - 1, by, bz, false);
            boolean east = connectsTo(access, bx + 1, by, bz, false);
            addPaneOrFencePostAndArms(boxes, bx, by, bz, north, south, west, east, 1.5D, 0.25D);
            return boxes;
        }

        if (isCobbleWall(type)) {
            boolean north = connectsTo(access, bx, by, bz - 1, false);
            boolean south = connectsTo(access, bx, by, bz + 1, false);
            boolean west = connectsTo(access, bx - 1, by, bz, false);
            boolean east = connectsTo(access, bx + 1, by, bz, false);
            addPaneOrFencePostAndArms(boxes, bx, by, bz, north, south, west, east, 1.5D, 0.1875D);
            return boxes;
        }

        if (isSlab(type)) {
            boolean top = (data & 0x8) != 0;
            if (top) {
                boxes.add(new Box(bx, by + 0.5D, bz, bx + 1.0D, by + 1.0D, bz + 1.0D));
            } else {
                boxes.add(new Box(bx, by, bz, bx + 1.0D, by + 0.5D, bz + 1.0D));
            }
            return boxes;
        }

        if (isStairs(type)) {
            int rawData = data & 0xFF;
            int facing = rawData & 0x3;
            boolean upsideDown = (rawData & 0x4) != 0;
            double yMin = upsideDown ? 0.5D : 0.0D;
            double yMax = upsideDown ? 1.0D : 0.5D;
            double stepYMin = upsideDown ? 0.0D : 0.5D;
            double stepYMax = upsideDown ? 0.5D : 1.0D;

            boxes.add(new Box(bx, by + yMin, bz, bx + 1.0D, by + yMax, bz + 1.0D));
            if (facing == 0) {
                boxes.add(new Box(bx, by + stepYMin, bz + 0.5D, bx + 1.0D, by + stepYMax, bz + 1.0D));
            } else if (facing == 1) {
                boxes.add(new Box(bx, by + stepYMin, bz, bx + 1.0D, by + stepYMax, bz + 0.5D));
            } else if (facing == 2) {
                boxes.add(new Box(bx + 0.5D, by + stepYMin, bz, bx + 1.0D, by + stepYMax, bz + 1.0D));
            } else if (facing == 3) {
                boxes.add(new Box(bx, by + stepYMin, bz, bx + 0.5D, by + stepYMax, bz + 1.0D));
            }
            return boxes;
        }

        if (type == Material.SOUL_SAND) {
            boxes.add(new Box(bx, by, bz, bx + 1.0D, by + 0.875D, bz + 1.0D));
            return boxes;
        }

        if (type == Material.SNOW) {
            boxes.add(new Box(bx, by, bz, bx + 1.0D, by + 0.125D, bz + 1.0D));
            return boxes;
        }

        if (isCarpet(type)) {
            boxes.add(new Box(bx, by, bz, bx + 1.0D, by + 0.0625D, bz + 1.0D));
            return boxes;
        }

        if (type == Material.ENCHANTMENT_TABLE) {
            boxes.add(new Box(bx, by, bz, bx + 1.0D, by + 0.75D, bz + 1.0D));
            return boxes;
        }

        if (type == Material.BED_BLOCK) {
            boxes.add(new Box(bx, by, bz, bx + 1.0D, by + 0.5625D, bz + 1.0D));
            return boxes;
        }

        if (type == Material.CACTUS) {
            boxes.add(new Box(bx + 0.0625D, by, bz + 0.0625D, bx + 0.9375D, by + 1.0D, bz + 0.9375D));
            return boxes;
        }

        if (type == Material.CHEST || type == Material.TRAPPED_CHEST || type == Material.ENDER_CHEST) {
            boxes.add(new Box(bx + 0.0625D, by, bz + 0.0625D, bx + 0.9375D, by + 0.875D, bz + 0.9375D));
            return boxes;
        }

        if (type == Material.CAULDRON) {
            boxes.add(new Box(bx, by, bz, bx + 1.0D, by + 0.3125D, bz + 1.0D));
            boxes.add(new Box(bx, by, bz, bx + 0.125D, by + 1.0D, bz + 1.0D));
            boxes.add(new Box(bx + 0.875D, by, bz, bx + 1.0D, by + 1.0D, bz + 1.0D));
            boxes.add(new Box(bx, by, bz, bx + 1.0D, by + 1.0D, bz + 0.125D));
            boxes.add(new Box(bx, by, bz + 0.875D, bx + 1.0D, by + 1.0D, bz + 1.0D));
            return boxes;
        }

        if (type == Material.BREWING_STAND) {
            boxes.add(new Box(bx + 0.4375D, by, bz + 0.4375D, bx + 0.5625D, by + 0.875D, bz + 0.5625D));
            boxes.add(new Box(bx, by, bz, bx + 1.0D, by + 0.125D, bz + 1.0D));
            return boxes;
        }

        if (type.name().contains("DOOR") || type.name().contains("TRAP_DOOR") || type.name().contains("TRAPDOOR")) {
            boxes.add(new Box(bx, by, bz, bx + 1.0D, by + 1.0D, bz + 1.0D));
            return boxes;
        }

        if (type.isSolid()) {
            boxes.add(new Box(bx, by, bz, bx + 1.0D, by + 1.0D, bz + 1.0D));
        }
        return boxes;
    }

    public static boolean isThinCollision(Material material) {
        return isPane(material) || isFence(material) || isCobbleWall(material);
    }

    public static double getTopHeight(Material material, byte data) {
        if (isSlab(material)) {
            return (data & 0x8) != 0 ? 1.0D : 0.5D;
        }
        if (material == Material.SOUL_SAND) {
            return 0.875D;
        }
        if (material == Material.SNOW) {
            return 0.125D;
        }
        if (isCarpet(material)) {
            return 0.0625D;
        }
        if (material == Material.ENCHANTMENT_TABLE) {
            return 0.75D;
        }
        if (material == Material.BED_BLOCK) {
            return 0.5625D;
        }
        return 1.0D;
    }

    private static void addPaneOrFencePostAndArms(List<Box> boxes, int x, int y, int z,
            boolean north, boolean south, boolean west, boolean east, double height, double armHalfWidth) {
        double center = 0.5D;
        double postMin = center - armHalfWidth;
        double postMax = center + armHalfWidth;
        boxes.add(new Box(x + postMin, y, z + postMin, x + postMax, y + height, z + postMax));

        if (north) {
            boxes.add(new Box(x + postMin, y, z, x + postMax, y + height, z + postMin));
        }
        if (south) {
            boxes.add(new Box(x + postMin, y, z + postMax, x + postMax, y + height, z + 1.0D));
        }
        if (west) {
            boxes.add(new Box(x, y, z + postMin, x + postMin, y + height, z + postMax));
        }
        if (east) {
            boxes.add(new Box(x + postMax, y, z + postMin, x + 1.0D, y + height, z + postMax));
        }
    }

    private static boolean connectsTo(BlockAccess access, int x, int y, int z, boolean paneMode) {
        if (access == null) {
            return false;
        }
        LegacyBlockState relative = access.getBlockState(x, y, z);
        Material type = relative == null ? Material.AIR : relative.getType();
        if (type == Material.AIR) {
            return false;
        }
        if (paneMode) {
            return isPane(type) || type.isSolid();
        }
        return isFence(type) || isCobbleWall(type) || type.isSolid();
    }

    private static boolean isPane(Material type) {
        String name = type.name();
        return "THIN_GLASS".equals(name) || "GLASS_PANE".equals(name) || type == Material.IRON_FENCE;
    }

    private static boolean isFence(Material type) {
        String name = type.name();
        return "FENCE".equals(name) || name.endsWith("_FENCE");
    }

    private static boolean isCobbleWall(Material type) {
        return type == Material.COBBLE_WALL;
    }

    private static boolean isSlab(Material type) {
        String name = type.name();
        return name.contains("STEP") || name.contains("SLAB");
    }

    private static boolean isStairs(Material type) {
        return type.name().contains("STAIRS") || type.name().contains("STAIR");
    }

    private static boolean isCarpet(Material type) {
        return "CARPET".equals(type.name());
    }
}
