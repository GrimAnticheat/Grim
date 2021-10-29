package ac.grim.grimac.utils.latency;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.nmsutil.XMaterial;
import io.github.retrooper.packetevents.utils.player.ClientVersion;

import java.util.concurrent.ConcurrentHashMap;

public class CompensatedElytra {
    private final ConcurrentHashMap<Integer, Boolean> lagCompensatedIsGlidingMap = new ConcurrentHashMap<>();
    private final GrimPlayer player;
    public int lastToggleElytra = 1;
    public int lastToggleFly = 1;

    public CompensatedElytra(GrimPlayer player) {
        this.player = player;

        if (!XMaterial.supports(9))
            return;

        this.lagCompensatedIsGlidingMap.put((int) Short.MIN_VALUE, player.bukkitPlayer.isGliding());
    }

    public boolean isGlidingLagCompensated(int lastTransaction) {
        if (!XMaterial.supports(9))
            return false;

        return LatencyUtils.getBestValue(lagCompensatedIsGlidingMap, lastTransaction) && player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_9);
    }

    public void tryAddStatus(int transaction, boolean isGliding) {
        if (!XMaterial.supports(9))
            return;

        lagCompensatedIsGlidingMap.put(transaction, isGliding);
    }
}
