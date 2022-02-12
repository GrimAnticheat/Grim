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
            player.sendTrans = false;
            boolean flat = PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_13);

            if (player.bukkitPlayer == null) return;

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

            player.sendTrans = true;
        });
    }
}
