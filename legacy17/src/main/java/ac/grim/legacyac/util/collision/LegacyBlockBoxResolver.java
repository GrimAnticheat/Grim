package ac.grim.legacyac.util.collision;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.block.Block;

public final class LegacyBlockBoxResolver {
    private LegacyBlockBoxResolver() {
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
        List<Box> boxes = new ArrayList<Box>();
        Material type = block.getType();
        if (type == Material.AIR || type == Material.WATER || type == Material.STATIONARY_WATER
            || type == Material.LAVA || type == Material.STATIONARY_LAVA || type == Material.WEB) {
            return boxes;
        }

        int bx = block.getX();
        int by = block.getY();
        int bz = block.getZ();

        // Priority path: Grim-like partial collision shapes first.
        if (isPane(type)) {
            boolean north = connectsTo(block, 0, 0, -1, true);
            boolean south = connectsTo(block, 0, 0, 1, true);
            boolean west = connectsTo(block, -1, 0, 0, true);
            boolean east = connectsTo(block, 1, 0, 0, true);
            addPaneOrFencePostAndArms(boxes, bx, by, bz, north, south, west, east, 1.0D, 0.125D);
            return boxes;
        }

        if (isFence(type)) {
            boolean north = connectsTo(block, 0, 0, -1, false);
            boolean south = connectsTo(block, 0, 0, 1, false);
            boolean west = connectsTo(block, -1, 0, 0, false);
            boolean east = connectsTo(block, 1, 0, 0, false);
            addPaneOrFencePostAndArms(boxes, bx, by, bz, north, south, west, east, 1.5D, 0.25D);
            return boxes;
        }

        if (isCobbleWall(type)) {
            boolean north = connectsTo(block, 0, 0, -1, false);
            boolean south = connectsTo(block, 0, 0, 1, false);
            boolean west = connectsTo(block, -1, 0, 0, false);
            boolean east = connectsTo(block, 1, 0, 0, false);
            // 1.7 walls have a narrower center post than fences.
            addPaneOrFencePostAndArms(boxes, bx, by, bz, north, south, west, east, 1.5D, 0.1875D);
            return boxes;
        }

        if (isSlab(type)) {
            boolean top = (block.getData() & 0x8) != 0;
            if (top) {
                boxes.add(new Box(bx, by + 0.5D, bz, bx + 1.0D, by + 1.0D, bz + 1.0D));
            } else {
                boxes.add(new Box(bx, by, bz, bx + 1.0D, by + 0.5D, bz + 1.0D));
            }
            return boxes;
        }

        if (isStairs(type)) {
            int rawData = block.getData() & 0xFF;
            int data = rawData & 0x3;
            boolean upsideDown = (rawData & 0x4) != 0;
            double yMin = upsideDown ? 0.5D : 0.0D;
            double yMax = upsideDown ? 1.0D : 0.5D;
            double stepYMin = upsideDown ? 0.0D : 0.5D;
            double stepYMax = upsideDown ? 0.5D : 1.0D;

            boxes.add(new Box(bx, by + yMin, bz, bx + 1.0D, by + yMax, bz + 1.0D));
            if (data == 0) {
                boxes.add(new Box(bx, by + stepYMin, bz + 0.5D, bx + 1.0D, by + stepYMax, bz + 1.0D));
            } else if (data == 1) {
                boxes.add(new Box(bx, by + stepYMin, bz, bx + 1.0D, by + stepYMax, bz + 0.5D));
            } else if (data == 2) {
                boxes.add(new Box(bx + 0.5D, by + stepYMin, bz, bx + 1.0D, by + stepYMax, bz + 1.0D));
            } else if (data == 3) {
                boxes.add(new Box(bx, by + stepYMin, bz, bx + 0.5D, by + stepYMax, bz + 1.0D));
            }
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

    private static boolean connectsTo(Block source, int dx, int dy, int dz, boolean paneMode) {
        Block relative = source.getRelative(dx, dy, dz);
        Material type = relative.getType();
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
}
