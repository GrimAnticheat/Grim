package ac.grim.grimac.platform.bukkit.world;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.platform.api.Platform;
import ac.grim.grimac.platform.api.world.PlatformChunk;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import lombok.RequiredArgsConstructor;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class BukkitPlatformChunk implements PlatformChunk {
    private static final boolean CUSTOMIZABLE_WORLD_HEIGHT = PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_17);
    private static final Map<BlockData, Integer> blockDataToId = GrimAPI.INSTANCE.getPlatform() == Platform.FOLIA ? new ConcurrentHashMap<>() : new HashMap<>();
    private static final boolean isFlat = PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_13);
    private final @NotNull Chunk chunk;

    @Override
    public int getBlockID(int x, int y, int z) {
        if (isIllegalPosition(chunk.getWorld(), x, y, z)) {
            return WrappedBlockState.getDefaultState(StateTypes.AIR).getGlobalId();
        }

        Block block = chunk.getBlock(x, y, z);

        return isFlat // Cache blockDataToID because Strings are expensive
                ? blockDataToId.computeIfAbsent(block.getBlockData(), data -> WrappedBlockState.getByString(PacketEvents.getAPI().getServerManager().getVersion().toClientVersion(), data.getAsString(false)).getGlobalId())
                : getLegacyBlockID(block);
    }

    @SuppressWarnings({ "deprecation", "UnstableApiUsage" })
    private static int getLegacyBlockID(@NotNull Block block) {
        return (block.getType().getId() << 4) | block.getData();
    }

    public static boolean isIllegalY(@NotNull World world, int y) {
        int minY = CUSTOMIZABLE_WORLD_HEIGHT ? world.getMinHeight() : 0;
        int maxY = CUSTOMIZABLE_WORLD_HEIGHT ? world.getMaxHeight() : 255;
        return minY > y || y > maxY;
    }

    public static boolean isIllegalPosition(World world, int x, int y, int z) {
        return isIllegalY(world, y) || x < 0 || x > 15 || z < 0 || z > 15;
    }
}
