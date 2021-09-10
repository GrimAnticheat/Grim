package ac.grim.grimac.utils.latency;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.nmsImplementations.XMaterial;
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

        // Mojang is terrible at security and sends a gliding = true, gliding = false if the player lies about having an elytra
        // This fixes that security issue of sending that the player is gliding when the player can't glide at all!
        // Thanks mojang...
        if (!isGliding) { // if the current one is false
            Boolean lastTransFlying = lagCompensatedIsGlidingMap.get(transaction - 1);
            // and was immediately sent after sending the client true
            if (lastTransFlying != null && lastTransFlying) {
                // discard the true value because vanilla sent it to tell the client that they can't glide,
                // and this situation only occurs if the player has a client (future bad packets check?)
                lagCompensatedIsGlidingMap.remove(transaction - 1);
            }
        }

        lagCompensatedIsGlidingMap.put(transaction, isGliding);
    }
}
