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

public class CompensatedKnockback {
    ConcurrentHashMap<Integer, ConcurrentList<Vector>> requiredKnockback = new ConcurrentHashMap<>();
    ConcurrentHashMap<Integer, ConcurrentList<Vector>> optionalKnockback = new ConcurrentHashMap<>();
    GrimPlayer grimPlayer;

    public CompensatedKnockback(GrimPlayer grimPlayer) {
        this.grimPlayer = grimPlayer;
    }

    public void addPlayerKnockback(Vector knockback) {
        int lastTransactionSent = grimPlayer.lastTransactionSent.get();

        if (!requiredKnockback.containsKey(lastTransactionSent)) {
            requiredKnockback.put(lastTransactionSent, new ConcurrentList<>());
        }

        requiredKnockback.get(lastTransactionSent).add(knockback);
    }

    public void setPlayerKnockbackApplied(Vector knockback) {
        // TODO:
    }

    public List<Vector> getPossibleKnockback(int lastTransactionReceived) {
        List<Vector> knockbackList = new ArrayList<>();

        Iterator<Map.Entry<Integer, ConcurrentList<Vector>>> iterator = requiredKnockback.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, ConcurrentList<Vector>> knockback = iterator.next();

            // 20 is minimum ticks per movement packet
            if (knockback.getKey() - 20 > lastTransactionReceived) continue;

            if (knockback.getKey() < grimPlayer.lastTransactionReceived) {
                Bukkit.broadcastMessage("Player ignored kb!");
                iterator.remove();
                continue;
            }

            knockbackList.addAll(knockback.getValue());
        }

        return knockbackList;
    }
}
