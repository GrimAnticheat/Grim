package ac.grim.grimac.checks.impl.autoclicker;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.lists.EvictingQueue;
import ac.grim.grimac.utils.math.GrimMath;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import org.jetbrains.annotations.NotNull;

@CheckData(name = "AutoClickerD", experimental = false, description = "Detects extremely regular high CPS patterns typical of drag/jitter click")
public class AutoClickerD extends Check implements PacketCheck {

    private EvictingQueue<Long> clickIntervals;
    private long lastClickTime = -1L;
    private boolean digging = false;

    private double cpsThreshold;
    private double stdDevThreshold;
    private double bufferIncrease;
    private double bufferDecrease;
    private double bufferTrigger;
    private int sampleSize;

    private double buffer = 0.0;

    public AutoClickerD(@NotNull GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        // Detect block breaking
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            WrapperPlayClientPlayerDigging dig = new WrapperPlayClientPlayerDigging(event);
            switch (dig.getAction()) {
                case START_DIGGING -> digging = true;
                case FINISHED_DIGGING, CANCELLED_DIGGING,
                     DROP_ITEM, DROP_ITEM_STACK,
                     SWAP_ITEM_WITH_OFFHAND -> digging = false;
            }
            return;
        }

        // Only process animation packets (clicks)
        if (event.getPacketType() != PacketType.Play.Client.ANIMATION) return;
        
        // Ignore during mining or recent attacks
        if (digging || player.actionManager.hasAttackedSince(500)) return;

        long now = System.currentTimeMillis();

        if (lastClickTime != -1L) {
            long interval = now - lastClickTime;

            // Only process reasonable intervals (10ms to 500ms = 2-100 CPS)
            if (interval >= 10 && interval <= 500) {
                clickIntervals.add(interval);

                if (clickIntervals.isCollected()) {
                    analyzePattern();
                    // Clear to analyze new patterns
                    clickIntervals.clear();
                }
            }
        }

        lastClickTime = now;
    }

    private void analyzePattern() {
        double avgInterval = GrimMath.getAverageLong(clickIntervals);
        double stdDev = GrimMath.getStandardDeviationLong(clickIntervals);
        double cps = 1000.0 / avgInterval;

        // Detect highly consistent, non-human clicking patterns
        // High CPS + very low standard deviation = suspicious consistency
        if (cps > cpsThreshold && stdDev < stdDevThreshold) {
            buffer += (stdDevThreshold - stdDev) * bufferIncrease;
        } else {
            buffer = Math.max(0, buffer - bufferDecrease);
        }

        // Trigger detection
        if (buffer > bufferTrigger) {
            flagAndAlert(String.format(
                "type=drag_jitter cps=%.1f dev=%.3f buf=%.1f",
                cps, stdDev, buffer
            ));
            
            // Soft reset after detection
            buffer *= 0.5;
            
            // Optional: Clear less samples to maintain some history
            if (clickIntervals.size() > sampleSize / 2) {
                clickIntervals.clear();
            }
        }
    }

    @Override
    public void onReload(ConfigManager config) {
        this.sampleSize = config.getIntElse(getConfigName() + ".sample-size", 35);
        this.cpsThreshold = config.getDoubleElse(getConfigName() + ".cps-threshold", 14.0);
        this.stdDevThreshold = config.getDoubleElse(getConfigName() + ".stddev-threshold", 2.0);
        this.bufferIncrease = config.getDoubleElse(getConfigName() + ".buffer-increase", 1.5);
        this.bufferDecrease = config.getDoubleElse(getConfigName() + ".buffer-decrease", 0.4);
        this.bufferTrigger = config.getDoubleElse(getConfigName() + ".buffer-trigger", 8.0);

        this.clickIntervals = new EvictingQueue<>(sampleSize);
        this.lastClickTime = -1L;
        this.buffer = 0.0;
        this.digging = false;
    }
}
