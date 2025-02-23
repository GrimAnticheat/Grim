package ac.grim.bukkit.world;

import ac.grim.grimac.platform.api.world.PlatformChunk;
import ac.grim.grimac.platform.api.world.PlatformWorld;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Getter
public class BukkitPlatformWorld implements PlatformWorld {

    private final World bukkitWorld;

    public BukkitPlatformWorld(@NotNull World world) {
        this.bukkitWorld = world;
    }

    @Override
    public boolean isChunkLoaded(int i, int i1) {
        return bukkitWorld.isChunkLoaded(i, i1);
    }

    @Override
    public WrappedBlockState getBlockAt(int i, int j, int k) {
        return SpigotConversionUtil.fromBukkitBlockData(bukkitWorld.getBlockAt(i, j, k).getBlockData());
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
