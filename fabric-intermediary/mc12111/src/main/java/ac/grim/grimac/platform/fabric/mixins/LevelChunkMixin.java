package ac.grim.grimac.platform.fabric.mixins;

import ac.grim.grimac.platform.api.world.PlatformChunk;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LevelChunk.class)
@Implements(@Interface(iface = PlatformChunk.class, prefix = "grimac$"))
abstract class LevelChunkMixin {
    // TODO (Fabric) (Region Threading) use ThreadLocal for Fabric region threading mods instead of a single variable
    // Having a single grimac$sharedPos works when the server is run on the single thread, as vanilla does
    @Unique
    private static final BlockPos.MutableBlockPos grimac$sharedPos = new BlockPos.MutableBlockPos();

    public int grimac$getBlockID(int x, int y, int z) {
        LevelChunk chunk = (LevelChunk) (Object) this;
        grimac$sharedPos.set(chunk.getPos().getMinBlockX() + x, y, chunk.getPos().getMinBlockZ() + z);
        return Block.getId(chunk.getBlockState(grimac$sharedPos));
    }
}
