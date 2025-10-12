package ac.grim.grimac.platform.bukkit.world;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.platform.api.Platform;
import ac.grim.grimac.platform.api.world.PlatformChunk;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import lombok.RequiredArgsConstructor;
import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class BukkitPlatformChunk implements PlatformChunk {
    private static final Map<BlockData, Integer> blockDataToId = GrimAPI.INSTANCE.getPlatform() == Platform.FOLIA ? new ConcurrentHashMap<>() : new HashMap<>();
    private static final boolean isFlat = PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_13);
    private final @NotNull Chunk chunk;

    @Override
    public int getBlockID(int x, int y, int z) {
        Block block = chunk.getBlock(x, y, z);

        return isFlat // Cache blockDataToID because Strings are expensive
                ? blockDataToId.computeIfAbsent(block.getBlockData(), data -> WrappedBlockState.getByString(PacketEvents.getAPI().getServerManager().getVersion().toClientVersion(), data.getAsString(false)).getGlobalId())
                : getLegacyBlockID(block);
    }

    @SuppressWarnings({ "deprecation", "UnstableApiUsage" })
    private static int getLegacyBlockID(@NotNull Block block) {
        return (block.getType().getId() << 4) | block.getData();
    }
}
