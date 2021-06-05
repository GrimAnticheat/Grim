package ac.grim.grimac.utils.latency;

import ac.grim.grimac.player.GrimPlayer;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CompensatedElytra {
    private final ConcurrentHashMap<Integer, Boolean> lagCompensatedIsGlidingMap = new ConcurrentHashMap<>();
    private final GrimPlayer player;
    public int lastToggleElytra = 1;

    public CompensatedElytra(GrimPlayer player) {
        this.player = player;
        this.lagCompensatedIsGlidingMap.put((int) Short.MIN_VALUE, player.bukkitPlayer.isGliding());
    }

    public boolean isGlidingLagCompensated(int lastTransaction) {
        return LatencyUtils.getBestValue(lagCompensatedIsGlidingMap, lastTransaction) && player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_9);
    }

    public void tryAddStatus(int transaction, boolean isGliding) {
        lagCompensatedIsGlidingMap.put(transaction, isGliding);
    }
}
