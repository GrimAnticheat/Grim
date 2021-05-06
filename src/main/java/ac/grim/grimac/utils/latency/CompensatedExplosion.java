package ac.grim.grimac.utils.latency;

import ac.grim.grimac.player.GrimPlayer;
import io.github.retrooper.packetevents.utils.list.ConcurrentList;
import org.bukkit.Bukkit;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CompensatedExplosion {
    ConcurrentHashMap<Integer, ConcurrentList<Vector>> requiredExplosion = new ConcurrentHashMap<>();
    GrimPlayer player;

    public CompensatedExplosion(GrimPlayer player) {
        this.player = player;
    }

    public void addPlayerExplosion(double x, double y, double z) {
        if (x == 0 && y == 0 && z == 0) return;

        int lastTransactionSent = player.lastTransactionSent.get();

        if (!requiredExplosion.containsKey(lastTransactionSent)) {
            requiredExplosion.put(lastTransactionSent, new ConcurrentList<>());
        }

        requiredExplosion.get(lastTransactionSent).add(new Vector(x, y, z));
    }

    public void setExplosionApplied(Vector knockback) {
        // TODO:
    }

    public List<Vector> getPossibleExplosions(int lastTransactionReceived) {
        List<Vector> explosionList = new ArrayList<>();

        Iterator<Map.Entry<Integer, ConcurrentList<Vector>>> iterator = requiredExplosion.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, ConcurrentList<Vector>> explosion = iterator.next();

            // 20 is minimum ticks per movement packet
            if (explosion.getKey() - 20 > lastTransactionReceived) continue;

            if (explosion.getKey() < player.lastTransactionReceived) {
                Bukkit.broadcastMessage("Player ignored explosion!");
                iterator.remove();
                continue;
            }

            explosionList.addAll(explosion.getValue());
        }

        return explosionList;
    }
}
