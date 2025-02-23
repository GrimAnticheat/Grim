package ac.grim.bukkit.world;

import ac.grim.grimac.platform.api.world.PlatformChunk;
import ac.grim.grimac.platform.api.world.PlatformWorld;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
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
    public Block getBlockAt(int i, int j, int k) {
        return bukkitWorld.getBlockAt(i, j, k);
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
