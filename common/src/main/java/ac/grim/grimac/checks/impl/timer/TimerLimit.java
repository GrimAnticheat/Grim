package ac.grim.grimac.checks.impl.timer;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import org.jetbrains.annotations.NotNull;

// This works around 1.3 timer, to prevent too high abuse - maybe there's a better solution?
@CheckData(name = "TimerLimit", stableKey = "grim.timer.limit", description = "The player has sent too many packets after high latency", setback = 10)
public class TimerLimit extends Timer {

    // At what ping should we start to limit the balance advantage? (nanos)
    private long limitAbuseOverPing;

    public TimerLimit(GrimPlayer player) {
        super(player);
    }

    @Override
    public void doCheck(final PacketReceiveEvent event) {
        // 1:1 with Timer minus cancelling the packet
        if (timerBalanceRealTime > System.nanoTime()) {
            // If timer check already flagged, don't flag.
            if (!event.isCancelled()) {
                if (flag() && shouldSetback()) {
                    player.getSetbackTeleportUtil().executeNonSimulatingSetback();
                }
            }

            // Reset the violation by 1 movement
            timerBalanceRealTime -= 50e6;
        }

        limitFallBehind();
    }

    @Override
    protected void limitFallBehind() {
        // Limit using transaction ping if over 1000ms (default)
        long playerClock = lastMovementPlayerClock;
        // Read before the stall check, so a stall right after it cannot move the limit forward
        long now = System.nanoTime();
        // A server stall looks like player latency until the player answers a transaction sent after it
        if (limitAbuseOverPing != -1 && now - playerClock > limitAbuseOverPing
                && !GrimAPI.INSTANCE.getServerStall().hasStalledSince(playerClock)) {
            playerClock = now - limitAbuseOverPing;
        }
        timerBalanceRealTime = Math.max(timerBalanceRealTime, playerClock - clockDrift);
    }

    @Override
    public void onReload(@NotNull ConfigManager config) {
        super.onReload(config);
        limitAbuseOverPing = config.getLongElse(getConfigName() + ".ping-abuse-limit-threshold", 1000L);
        if (limitAbuseOverPing != -1) {
            limitAbuseOverPing *= (long) 1e6;
        }
    }
}
