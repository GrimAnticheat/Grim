package ac.grim.grimac.utils.latency;

import ac.grim.grimac.player.GrimPlayer;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Flying status is just really. really. complicated.  You shouldn't need to touch this, but if you do -
// Don't let the player fly with packets
// Accept even if bukkit says the player can't fly lag might allow them to
// Accept that the server can change the player's packets without an update response from the player
// Accept that the player's flying status lies when landing on the ground (Worked around in GrimPlayer.specialFlying)
//
// This isn't perfect but it's damn close and should be the best public open source flight lag compensation system
public class CompensatedFlying {
    private final ConcurrentHashMap<Integer, Boolean> lagCompensatedCanFlyMap = new ConcurrentHashMap<>();
    private final GrimPlayer player;

    public ConcurrentHashMap<Integer, Boolean> lagCompensatedIsFlyingMap = new ConcurrentHashMap<>();

    public CompensatedFlying(GrimPlayer player) {
        this.player = player;
        lagCompensatedCanFlyMap.put((int) Short.MIN_VALUE, player.bukkitPlayer.getAllowFlight());
        lagCompensatedIsFlyingMap.put((int) Short.MIN_VALUE, player.bukkitPlayer.isFlying());
    }

    public void setCanPlayerFly(boolean canFly) {
        lagCompensatedCanFlyMap.put(player.lastTransactionSent.get(), canFly);
    }

    public boolean canFlyLagCompensated() {
        // Looking one in the future is generally more accurate
        // We have to calculate our own values because bukkit isn't lag compensated

        // Bukkit is all caught up, use it's value in case of desync
        // I can't figure out how it would desync but just to be safe...
        if (lagCompensatedIsFlyingMap.size() == 1 && lagCompensatedCanFlyMap.size() == 1)
            return player.bukkitPlayer.isFlying();

        // Prevent players messing with abilities packets to bypass anticheat
        if (!getBestValue(lagCompensatedCanFlyMap, player.lastTransactionReceived))
            return false;

        return getBestValue(lagCompensatedIsFlyingMap, player.packetStateData.packetLastTransactionReceived);
    }

    private boolean getBestValue(ConcurrentHashMap<Integer, Boolean> hashMap, int lastTransactionReceived) {
        int bestKey = Integer.MIN_VALUE;
        // This value is always set because one value is always left in the maps
        boolean bestValue = false;

        Iterator<Map.Entry<Integer, Boolean>> iterator = hashMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Boolean> flightStatus = iterator.next();

            if (flightStatus.getKey() > lastTransactionReceived) continue;

            if (flightStatus.getKey() < bestKey) {
                iterator.remove();
                continue;
            }

            bestKey = flightStatus.getKey();
            bestValue = flightStatus.getValue();
        }

        return bestValue;
    }
}
