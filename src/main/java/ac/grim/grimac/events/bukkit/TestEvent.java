package ac.grim.grimac.events.bukkit;

import ac.grim.grimac.utils.chunks.ChunkCache;
import net.minecraft.server.v1_16_R3.Block;
import net.minecraft.server.v1_16_R3.IBlockData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class TestEvent implements Listener {
    @EventHandler
    public void onPlayerMoveEvent(PlayerMoveEvent event) {
        Location playerLocation = event.getPlayer().getLocation();
        int playerX = playerLocation.getBlockX();
        int playerY = playerLocation.getBlockY() - 1;
        int playerZ = playerLocation.getBlockZ();

        int block = ChunkCache.getBlockAt(playerX, playerY, playerZ);

        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    Block.getByCombinedId(ChunkCache.getBlockAt(playerX >> 4 << 4 + x, y, playerX >> 4 << 4 + z));
                }
            }
        }

        IBlockData nmsBlock = Block.getByCombinedId(block);
        Bukkit.broadcastMessage("The player is standing on " + nmsBlock.getBlock().i());

    }
}
