package ac.grim.grimac.platform.bukkit.world;

import ac.grim.grimac.platform.api.world.PlatformChunk;
import ac.grim.grimac.platform.api.world.PlatformWorld;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record BukkitPlatformWorld(@NotNull World bukkitWorld) implements PlatformWorld {

    private static final boolean LEGACY_SERVER_VERSION = PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_12_2);

    @Override
    public boolean isChunkLoaded(int chunkX, int chunkZ) {
        return bukkitWorld.isChunkLoaded(chunkX, chunkZ);
    }

    @Override
    public WrappedBlockState getBlockAt(int x, int y, int z) {
        if (LEGACY_SERVER_VERSION) {
            Block block = bukkitWorld.getBlockAt(x, y, z);
            @SuppressWarnings({"deprecation", "UnstableApiUsage"})
            int blockId = (block.getType().getId() << 4) | block.getData();
            return WrappedBlockState.getByGlobalId(blockId);
        } else {
            if (BukkitPlatformChunk.isIllegalY(bukkitWorld, y)) return WrappedBlockState.getDefaultState(StateTypes.AIR);
            return SpigotConversionUtil.fromBukkitBlockData(bukkitWorld.getBlockAt(x, y, z).getBlockData());
        }
    }

    @Override
    public String getName() {
        return bukkitWorld.getName();
    }

    @Override
    public @NotNull UUID getUID() {
        return this.bukkitWorld.getUID();
    }

    @Override
    public PlatformChunk getChunkAt(int currChunkX, int currChunkZ) {
        return new BukkitPlatformChunk(bukkitWorld.getChunkAt(currChunkX, currChunkZ));
    }

    @Override
    public boolean isLoaded() {
        return Bukkit.getWorld(bukkitWorld.getUID()) != null;
    }
}
