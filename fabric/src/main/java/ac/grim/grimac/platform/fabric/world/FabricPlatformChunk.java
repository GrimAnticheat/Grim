package ac.grim.grimac.platform.fabric.world;

import ac.grim.grimac.platform.api.world.PlatformChunk;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.NotNull;

public class FabricPlatformChunk implements PlatformChunk {
    private final WorldChunk chunk;

    public FabricPlatformChunk(@NotNull WorldChunk chunk) {
        this.chunk = chunk;
    }

    @Override
    public int getBlockID(int x, int y, int z) {
        return Block.getRawIdFromState(chunk.getBlockState(new BlockPos(
                chunk.getPos().getStartX() + x,
                y,
                chunk.getPos().getStartZ() + z
        )));
    }
}
