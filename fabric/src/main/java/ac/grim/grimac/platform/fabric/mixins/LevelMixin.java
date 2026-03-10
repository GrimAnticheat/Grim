package ac.grim.grimac.platform.fabric.mixins;

import ac.grim.grimac.platform.api.world.PlatformChunk;
import ac.grim.grimac.platform.api.world.PlatformWorld;
import ac.grim.grimac.platform.fabric.GrimACFabricLoaderPlugin;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;

import java.util.UUID;

@Mixin(Level.class)
@Implements(@Interface(iface = PlatformWorld.class, prefix = "grimac$"))
abstract class LevelMixin implements LevelAccessor {

    @Shadow
    public abstract ResourceKey<Level> dimension();

    public boolean grimac$isChunkLoaded(int chunkX, int chunkZ) {
        return hasChunk(chunkX, chunkZ);
    }

    public WrappedBlockState grimac$getBlockAt(int x, int y, int z) {
        return WrappedBlockState.getByGlobalId(
                Block.getId(getBlockState(new BlockPos(x, y, z)))
        );
    }

    public String grimac$getName() {
        return this.dimension().location().toString();
    }

    public @Nullable UUID grimac$getUID() {
        throw new UnsupportedOperationException();
    }

    public PlatformChunk grimac$getChunkAt(int currChunkX, int currChunkZ) {
        return (PlatformChunk) getChunk(currChunkX, currChunkZ);
    }

    public boolean grimac$isLoaded() {
        return GrimACFabricLoaderPlugin.FABRIC_SERVER.getLevel(this.dimension()) != null;
    }
}
