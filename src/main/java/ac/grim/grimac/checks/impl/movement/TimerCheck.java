package ac.grim.grimac.checks.impl.movement;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "Timer", configName = "TimerA", setback = 10)
public class TimerCheck extends Check implements PacketCheck {
    long timerBalanceRealTime = 0;

    // Default value is real time minus max keep-alive time
    long knownPlayerClockTime = (long) (System.nanoTime() - 6e10);
    long lastMovementPlayerClock = (long) (System.nanoTime() - 6e10);

    // How long should the player be able to fall back behind their ping?
    // Default: 120 milliseconds
    long clockDrift;

    long limitAbuseOverPing;

    boolean hasGottenMovementAfterTransaction = false;

    // Proof for this timer check
    // https://i.imgur.com/Hk2Wb6c.png
    //
    // The largest gap will always be the transaction ping (server -> client -> server)
    // Proof lies that client -> server ping will always be lower
    //
    // The largest gap is the floor for movements
    // If the smaller gap surpasses the larger gap, the player is cheating
    //
    // This usually flags 1.01 on low ping extremely quickly
    // Higher ping/low fps scales proportionately, and will flag less quickly but will still always flag 1.01
    // Players standing still will reset this amount of time
    //
    // This is better than traditional timer checks because ping fluctuations will never affect this check
    // As we are tying this check to the player's ping, rather than real time.
    //
    // Tested 10/20/30 fps and f3 + t spamming for lag spikes at 0 ping localhost/200 ping clumsy, no falses
    // Also didn't false when going from 0 -> 2000 ms ping, and 2000 ms -> 0 ms ping
    // it's a very nice check, in my opinion.  I guess I will find out if netty lag can false it

    // You might notice that we deviate a bit from this to handle lag
    // We take the FIRST transaction after each movement, to avoid issues with this packet order at low FPS:
    // TRANSACTION TRANSACTION TRANSACTION MOVEMENT MOVEMENT MOVEMENT
    // TRANSACTION TRANSACTION TRANSACTION MOVEMENT MOVEMENT MOVEMENT
    //
    // We then take the last transaction before this to increase stability with these lag spikes and
    // to guarantee that we are at least 50 ms back before adding the time
    public TimerCheck(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (hasGottenMovementAfterTransaction && checkForTransaction(event.getPacketType())) {
            knownPlayerClockTime = lastMovementPlayerClock;
            lastMovementPlayerClock = player.getPlayerClockAtLeast();
            hasGottenMovementAfterTransaction = false;
        }

        if (!shouldCountPacketForTimer(event.getPacketType())) return;

        hasGottenMovementAfterTransaction = true;
        timerBalanceRealTime += 50e6;

        doCheck(event);
    }


    public void doCheck(final PacketReceiveEvent event) {
        final double transactionPing = player.getTransactionPing();
        // Limit using transaction ping if over 1000ms (default)
        final boolean needsAdjustment = limitAbuseOverPing != -1 && transactionPing >= limitAbuseOverPing;
        final boolean wouldFailNormal = timerBalanceRealTime > System.nanoTime();
        final boolean failsAdjusted = needsAdjustment && (timerBalanceRealTime + ((transactionPing * 1e6) - clockDrift - 50e6)) > System.nanoTime();
        if (wouldFailNormal || failsAdjusted) {
            if (flag()) {
                // Cancel the packet
                // Only cancel if not an adjustment setback
                if (wouldFailNormal && shouldModifyPackets()) {
                    event.setCancelled(true);
                    player.onPacketCancel();
                }

                if (isAboveSetbackVl()) player.getSetbackTeleportUtil().executeNonSimulatingSetback();

                if (wouldFailNormal) {
                    // Only alert if we would fail without adjusted limit
                    alert("");
                }
            }

            // Reset the violation by 1 movement
            timerBalanceRealTime -= 50e6;
        }

        timerBalanceRealTime = Math.max(timerBalanceRealTime, lastMovementPlayerClock - clockDrift);
    }

    public boolean checkForTransaction(PacketTypeCommon packetType) {
        return packetType == PacketType.Play.Client.PONG ||
                packetType == PacketType.Play.Client.WINDOW_CONFIRMATION;
    }

    public boolean shouldCountPacketForTimer(PacketTypeCommon packetType) {
        // If not flying, or this was a teleport, or this was a duplicate 1.17 mojang stupidity packet
        return WrapperPlayClientPlayerFlying.isFlying(packetType) &&
                !player.packetStateData.lastPacketWasTeleport && !player.packetStateData.lastPacketWasOnePointSeventeenDuplicate;
    }

    @Override
    public void onReload(ConfigManager config) {
        clockDrift = (long) (config.getDoubleElse(getConfigName() + ".drift", 120.0) * 1e6);
        limitAbuseOverPing = (long) (config.getDoubleElse(getConfigName() + ".ping-abuse-limit-threshold", 1000));
    }
}
