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

@CheckData(name = "AutoClickerE", experimental = true, description = "Detects standard clicking consistency patterns")
public class AutoClickerE extends Check implements PacketCheck {

    private EvictingQueue<Long> clickIntervals;
    private EvictingQueue<Double> cpsSamples;

    private long lastSwingTime = -1L;
    private boolean digging = false;

    // Configuration
    private int sampleSize;
    private double minCPSThreshold;
    private double maxCPSThreshold;
    private double consistencyThreshold;
    private double lowVarianceThreshold;
    private double bufferDecrement;
    private double bufferThreshold;
    private double lowVarIncrease;
    private double consistencyIncrease;

    // Detection buffers
    private double consistencyBuffer = 0.0;
    private double lowVarianceBuffer = 0.0;

    public AutoClickerE(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        // Detect when the player is mining blocks
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            WrapperPlayClientPlayerDigging dig = new WrapperPlayClientPlayerDigging(event);
            switch (dig.getAction()) {
                case START_DIGGING -> digging = true;
                case CANCELLED_DIGGING, FINISHED_DIGGING, DROP_ITEM_STACK, DROP_ITEM, SWAP_ITEM_WITH_OFFHAND -> digging = false;
            }
            return;
        }

        // Ignore swings during block mining or recent attacks
        if (event.getPacketType() != PacketType.Play.Client.ANIMATION) return;
        if (digging || player.actionManager.hasAttackedSince(500)) return;

        long now = System.currentTimeMillis();

        if (lastSwingTime != -1L) {
            long interval = now - lastSwingTime;

            // Only process reasonable intervals (between 20ms and 1000ms)
            if (interval >= 20 && interval <= 1000) {
                clickIntervals.add(interval);

                // Calculate current CPS
                double currentCPS = 1000.0 / interval;
                cpsSamples.add(currentCPS);
            }
        }

        lastSwingTime = now;

        // Check if we have collected enough samples to analyze
        if (clickIntervals.isCollected()) {
            analyzeConsistency();
        }
    }

    private void analyzeConsistency() {
        double avgCPS = GrimMath.getAverage(cpsSamples);
        double stdDevCPS = GrimMath.getStandardDeviation(cpsSamples);

        double avgInterval = GrimMath.getAverageLong(clickIntervals);
        double stdDevInterval = GrimMath.getStandardDeviationLong(clickIntervals);

        double cvInterval = (stdDevInterval / avgInterval) * 100.0; // Coefficient of variation (%)
        double cvCPS = (stdDevCPS / avgCPS) * 100.0;

        // Check if the average CPS is within the range we want to analyze
        if (avgCPS >= minCPSThreshold && avgCPS <= maxCPSThreshold) {

            // Detection 1: Perfect or near-perfect consistency (very low variance)
            if (cvInterval < lowVarianceThreshold) {
                lowVarianceBuffer += (lowVarianceThreshold - cvInterval) * lowVarIncrease;
            } else {
                lowVarianceBuffer = Math.max(0, lowVarianceBuffer - bufferDecrement);
            }

            // Detection 2: Standard consistency pattern (recognizable but artificial)
            if (cvInterval > consistencyThreshold && cvInterval < 25.0) {
                consistencyBuffer += (cvInterval - consistencyThreshold) * consistencyIncrease;
            } else {
                consistencyBuffer = Math.max(0, consistencyBuffer - bufferDecrement);
            }

            // Combine both detections
            double combinedBuffer = consistencyBuffer + lowVarianceBuffer;

            if (combinedBuffer > bufferThreshold) {
                flagAndAlert(String.format(
                    "type=consistency cps=%.1f cv=%.1f%% low_var=%.1f cons=%.1f total=%.1f",
                    avgCPS, cvInterval, lowVarianceBuffer, consistencyBuffer, combinedBuffer
                ));

                // Partial buffer reset
                consistencyBuffer *= 0.6;
                lowVarianceBuffer *= 0.6;

                // Clear some samples to avoid repeated detections
                if (clickIntervals.size() > sampleSize / 2) {
                    clickIntervals.clear();
                    cpsSamples.clear();
                }
            }
        } else {
            // Decay buffers when outside CPS range
            consistencyBuffer = Math.max(0, consistencyBuffer - bufferDecrement * 0.5);
            lowVarianceBuffer = Math.max(0, lowVarianceBuffer - bufferDecrement * 0.5);
        }
    }

    @Override
    public void onReload(ConfigManager config) {
        this.sampleSize = config.getIntElse(getConfigName() + ".sample-size", 35);
        this.minCPSThreshold = config.getDoubleElse(getConfigName() + ".min-cps-threshold", 8.0);
        this.maxCPSThreshold = config.getDoubleElse(getConfigName() + ".max-cps-threshold", 20.0);
        this.consistencyThreshold = config.getDoubleElse(getConfigName() + ".consistency-threshold", 8.0);
        this.lowVarianceThreshold = config.getDoubleElse(getConfigName() + ".low-variance-threshold", 3.5);
        this.bufferDecrement = config.getDoubleElse(getConfigName() + ".buffer-decrement", 0.15);
        this.bufferThreshold = config.getDoubleElse(getConfigName() + ".buffer-threshold", 4.5);
        this.lowVarIncrease = config.getDoubleElse(getConfigName() + ".low-var-increase", 0.1);
        this.consistencyIncrease = config.getDoubleElse(getConfigName() + ".consistency-increase", 0.05);

        this.clickIntervals = new EvictingQueue<>(sampleSize);
        this.cpsSamples = new EvictingQueue<>(sampleSize);
        this.lastSwingTime = -1L;
        this.consistencyBuffer = 0.0;
        this.lowVarianceBuffer = 0.0;
        this.digging = false;
    }
}
