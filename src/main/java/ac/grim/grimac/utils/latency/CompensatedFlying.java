package ac.grim.grimac.utils.latency;

import ac.grim.grimac.player.GrimPlayer;

import java.util.concurrent.ConcurrentHashMap;

// Flying status is just really. really. complicated.  You shouldn't need to touch this, but if you do -
// Don't let the player fly with packets
// Accept even if bukkit says the player can't fly lag might allow them to
// Accept that the server can change the player's packets without an update response from the player
// Accept that the player's flying status lies when landing on the ground (Worked around in GrimPlayer.specialFlying)
//
// This isn't perfect, but it's damn close and should be the best public open source flight lag compensation system
public class CompensatedFlying {
    private final ConcurrentHashMap<Integer, Boolean> lagCompensatedCanFlyMap = new ConcurrentHashMap<>();
    private final GrimPlayer player;

    public ConcurrentHashMap<Integer, Boolean> lagCompensatedIsFlyingMap = new ConcurrentHashMap<>();
    public int lastToggleTransaction = Integer.MIN_VALUE;

    public CompensatedFlying(GrimPlayer player) {
        this.player = player;
        lagCompensatedCanFlyMap.put((int) Short.MIN_VALUE, player.bukkitPlayer.getAllowFlight());
        lagCompensatedIsFlyingMap.put((int) Short.MIN_VALUE, player.bukkitPlayer.isFlying());
    }

    public void setCanPlayerFly(boolean canFly) {
        lagCompensatedCanFlyMap.put(player.lastTransactionSent.get() + 1, canFly);
    }

    public boolean canFlyLagCompensated(int lastTransaction) {
        boolean canFly = LatencyUtils.getBestValue(lagCompensatedCanFlyMap, lastTransaction);
        boolean isFlying = LatencyUtils.getBestValue(lagCompensatedIsFlyingMap, lastTransaction);

        // Prevent players messing with abilities packets to bypass anticheat
        if (!canFly)
            return false;

        return isFlying;
    }
}
