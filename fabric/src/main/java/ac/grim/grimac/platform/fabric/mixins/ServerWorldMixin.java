package ac.grim.grimac.platform.fabric.mixins;

import ac.grim.grimac.platform.api.world.PlatformChunk;
import ac.grim.grimac.platform.api.world.PlatformWorld;
import ac.grim.grimac.platform.fabric.GrimACFabricLoaderPlugin;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import net.minecraft.block.Block;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.level.ServerWorldProperties;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;

import java.util.UUID;

@Mixin(ServerWorld.class)
@Implements(@Interface(iface = PlatformWorld.class, prefix = "grimac$"))
abstract class ServerWorldMixin implements WorldAccess {
    @Shadow public @Final ServerWorldProperties worldProperties;

    public boolean grimac$isChunkLoaded(int chunkX, int chunkZ) {
        return isChunkLoaded(chunkX, chunkZ);
    }

    public WrappedBlockState grimac$getBlockAt(int x, int y, int z) {
        return WrappedBlockState.getByGlobalId(
                Block.getRawIdFromState(getBlockState(new BlockPos(x, y, z)))
        );
    }

    public String grimac$getName() {
        return worldProperties.getLevelName();
    }

    public @Nullable UUID grimac$getUID() {
        throw new UnsupportedOperationException();
    }

    public PlatformChunk grimac$getChunkAt(int currChunkX, int currChunkZ) {
        return (PlatformChunk) getChunk(currChunkX, currChunkZ);
    }

    public boolean grimac$isLoaded() {
        return GrimACFabricLoaderPlugin.FABRIC_SERVER.getWorld(getWorld().getRegistryKey()) != null;
    }
}
