package ac.grim.grimac.platform.fabric.mixins;

import ac.grim.grimac.platform.api.world.PlatformChunk;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(WorldChunk.class)
@Implements(@Interface(iface = PlatformChunk.class, prefix = "grimac$"))
abstract class WorldChunkMixin {
    public int grimac$getBlockID(int x, int y, int z) {
        WorldChunk chunk = (WorldChunk) (Object) this;
        return Block.getRawIdFromState(chunk.getBlockState(new BlockPos(
                chunk.getPos().getStartX() + x,
                y,
                chunk.getPos().getStartZ() + z
        )));
    }
}
