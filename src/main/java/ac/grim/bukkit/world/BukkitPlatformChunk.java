package ac.grim.bukkit.world;

import ac.grim.grimac.platform.api.world.PlatformChunk;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class BukkitPlatformChunk implements PlatformChunk {
    private static final HashMap<BlockData, Integer> blockDataToId = new HashMap<>();
    private static final boolean isFlat = PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_13);
    private final Chunk chunk;

    public BukkitPlatformChunk(@NotNull Chunk chunkAt) {
        this.chunk = chunkAt;
    }

    @Override
    public int getBlockID(int x, int y, int z) {
        Block block = chunk.getBlock(x, y, z);
        final int blockId;
        if (isFlat) {
            // Cache this because strings are expensive
            blockId = blockDataToId.computeIfAbsent(block.getBlockData(), data -> WrappedBlockState.getByString(PacketEvents.getAPI().getServerManager().getVersion().toClientVersion(), data.getAsString(false)).getGlobalId());
        } else {
            blockId = (block.getType().getId() << 4) | block.getData();
        }

        return blockId;
    }
}
