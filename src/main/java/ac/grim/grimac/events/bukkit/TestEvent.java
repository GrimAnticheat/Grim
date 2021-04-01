package ac.grim.grimac.events.bukkit;

import net.minecraft.server.v1_16_R3.BlockPosition;
import net.minecraft.server.v1_16_R3.IBlockData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_16_R3.CraftChunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.LinkedList;
import java.util.List;

public class TestEvent implements Listener {
    @EventHandler
    public void onPlayerMoveEvent(PlayerMoveEvent event) {
        Location playerLocation = event.getPlayer().getLocation();

        net.minecraft.server.v1_16_R3.Chunk chunk = ((CraftChunk) playerLocation.getWorld().getChunkAt(playerLocation)).getHandle();

        final List<IBlockData> materials = new LinkedList<>();

        Long startTime = System.nanoTime();

        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 128; y++) {
                for (int z = 0; z < 16; z++) {
                    //Block.getByCombinedId(ChunkCache.getBlockAt(playerX >> 4 << 4 + x, y, playerX >> 4 << 4 + z));
                    materials.add(chunk.getType(new BlockPosition(x, y, z)));
                }
            }
        }

        Bukkit.broadcastMessage("Reading chunks " + (System.nanoTime() - startTime) + " " + materials.size());

    }
}
