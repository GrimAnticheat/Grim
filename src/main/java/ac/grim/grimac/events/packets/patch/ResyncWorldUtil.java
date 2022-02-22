package ac.grim.grimac.events.packets.patch;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.math.GrimMath;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;

public class ResyncWorldUtil {
    public static void resyncPositions(GrimPlayer player, SimpleCollisionBox box) {
        resyncPositions(player, GrimMath.floor(box.minX), GrimMath.floor(box.minY), GrimMath.floor(box.minZ),
                GrimMath.ceil(box.maxX), GrimMath.ceil(box.maxY), GrimMath.ceil(box.maxZ));
    }

    public static void resyncPositions(GrimPlayer player, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        Bukkit.getScheduler().runTask(GrimAPI.INSTANCE.getPlugin(), () -> {
            boolean flat = PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_13);

            if (player.bukkitPlayer == null) return;
            // Player hasn't spawned, don't spam packets
            if (!player.getSetbackTeleportUtil().hasAcceptedSpawnTeleport) return;

            // Check the 4 corners of the BB for loaded chunks, don't freeze main thread to load chunks.
            if (!player.playerWorld.isChunkLoaded(minX >> 4, minZ >> 4) || !player.playerWorld.isChunkLoaded(minX >> 4, maxZ >> 4)
                    || !player.playerWorld.isChunkLoaded(maxX >> 4, minZ >> 4) || !player.playerWorld.isChunkLoaded(maxX >> 4, maxZ >> 4))
                return;

            player.sendTrans = false;

            try {
                for (int x = minX; x <= maxX; x++) {
                    for (int y = minY; y <= maxY; y++) {
                        for (int z = minZ; z <= maxZ; z++) {
                            Block block = player.bukkitPlayer.getWorld().getBlockAt(x, y, z);

                            if (flat) {
                                player.bukkitPlayer.sendBlockChange(new Location(player.bukkitPlayer.getWorld(), x, y, z), block.getBlockData());
                            } else {
                                player.bukkitPlayer.sendBlockChange(new Location(player.bukkitPlayer.getWorld(), x, y, z), block.getType(), block.getData());
                            }
                        }
                    }
                }
            } finally {
                player.sendTrans = true;
            }
        });
    }
}
