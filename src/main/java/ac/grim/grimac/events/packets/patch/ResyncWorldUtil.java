package ac.grim.grimac.events.packets.patch;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.blockstate.BaseBlockState;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.math.GrimMath;
import io.github.retrooper.packetevents.utils.pair.Pair;
import io.github.retrooper.packetevents.utils.vector.Vector3i;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.function.Predicate;

public class ResyncWorldUtil {
    public static void resyncPositions(GrimPlayer player, SimpleCollisionBox box, boolean likelyDesync) {
        resyncPositions(player, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, likelyDesync);
    }

    public static void resyncPositions(GrimPlayer player, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, boolean likelyDesync) {
        resyncPositions(player, GrimMath.floor(minX), GrimMath.floor(minY), GrimMath.floor(minZ),
                GrimMath.floor(maxX), GrimMath.floor(maxY), GrimMath.floor(maxZ), material -> true, likelyDesync);
    }

    public static void resyncPositions(GrimPlayer player, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, Predicate<Pair<BaseBlockState, Vector3i>> shouldSend, boolean likelyDesync) {
        Bukkit.getScheduler().runTask(GrimAPI.INSTANCE.getPlugin(), () -> {
            player.sendTrans = false;
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        player.bukkitPlayer.sendBlockChange(new Location(player.bukkitPlayer.getWorld(), x, y, z), player.bukkitPlayer.getWorld().getBlockData(x, y, z));
                    }
                }
            }
            player.sendTrans = true;
        });
    }
}
