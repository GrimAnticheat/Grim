package ac.grim.grimac.platform.fabric.mixins;

import ac.grim.grimac.platform.api.world.PlatformChunk;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LevelChunk.class)
@Implements(@Interface(iface = PlatformChunk.class, prefix = "grimac$"))
abstract class LevelChunkMixin {
    public int grimac$getBlockID(int x, int y, int z) {
        LevelChunk chunk = (LevelChunk) (Object) this;
        return Block.getId(chunk.getBlockState(new BlockPos(
                chunk.getPos().getMinBlockX() + x,
                y,
                chunk.getPos().getMinBlockZ() + z
        )));
    }
}
