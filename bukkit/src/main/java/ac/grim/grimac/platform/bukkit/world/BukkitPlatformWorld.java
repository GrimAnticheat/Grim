package ac.grim.grimac.platform.bukkit.world;

import ac.grim.grimac.platform.api.world.PlatformChunk;
import ac.grim.grimac.platform.api.world.PlatformWorld;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Getter
public class BukkitPlatformWorld implements PlatformWorld {

    private static final boolean LEGACY_SERVER_VERSION = PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_12_2);
    private final World bukkitWorld;

    public BukkitPlatformWorld(@NotNull World world) {
        this.bukkitWorld = world;
    }

    @Override
    public boolean isChunkLoaded(int chunkX, int chunkZ) {
        return bukkitWorld.isChunkLoaded(chunkX, chunkZ);
    }

    @Override
    public WrappedBlockState getBlockAt(int x, int y, int z) {
        if (LEGACY_SERVER_VERSION) {
            Block block = bukkitWorld.getBlockAt(x, y, z);
            int blockId = (block.getType().getId() << 4) | block.getData();
            return WrappedBlockState.getByGlobalId(blockId);
        } else {
            return SpigotConversionUtil.fromBukkitBlockData(bukkitWorld.getBlockAt(x, y, z).getBlockData());
        }
    }

    @Override
    public String getName() {
        return bukkitWorld.getName();
    }

    @Override
    public @Nullable UUID getUID() {
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
