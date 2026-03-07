package ac.grim.legacyac.world;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

public final class LegacyBlockState {
    public static final LegacyBlockState AIR = new LegacyBlockState(Material.AIR, (byte) 0);

    private final Material type;
    private final byte data;

    public LegacyBlockState(Material type, byte data) {
        this.type = type == null ? Material.AIR : type;
        this.data = data;
    }

    public Material getType() {
        return type;
    }

    public byte getData() {
        return data;
    }

    public boolean isAir() {
        return type == Material.AIR;
    }

    public boolean isLiquid() {
        return type == Material.WATER || type == Material.STATIONARY_WATER
                || type == Material.LAVA || type == Material.STATIONARY_LAVA;
    }

    public boolean isSolid() {
        return type.isSolid();
    }

    public static LegacyBlockState fromBlock(Block block) {
        if (block == null) {
            return AIR;
        }
        return new LegacyBlockState(block.getType(), block.getData());
    }

    public static LegacyBlockState fromWorld(World world, int x, int y, int z) {
        if (world == null || y < 0 || y > 255) {
            return AIR;
        }
        return fromBlock(world.getBlockAt(x, y, z));
    }
}
