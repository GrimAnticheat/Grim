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
import org.jetbrains.annotations.NotNull;

@CheckData(name = "AutoClickerE", experimental = true, description = "Detects standard clicking consistency patterns")
public class AutoClickerE extends Check implements PacketCheck {

    private EvictingQueue<Long> clickIntervals;
    private EvictingQueue<Double> cpsSamples;

    private int sampleSize;
    private double minCPSThreshold;
    private double maxCPSThreshold;
    private double consistencyThreshold;
    private double lowVarianceThreshold;
    private double bufferDecrement;
    private double bufferThreshold;
    private double lowVarIncrease;
    private double consistencyIncrease;

    private double consistencyBuffer = 0.0;
    private double lowVarianceBuffer = 0.0;

    public AutoClickerE(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.ANIMATION) return;

        if (player.packetOrderProcessor.isDigging() || player.actionManager.hasAttackedSince(500)) return;

        long interval = player.clickData.getDelay();

        if (interval >= 20 && interval <= 1000) {
            clickIntervals.add(interval);
            double currentCPS = player.clickData.getCps();
            if (currentCPS > 0) {
                cpsSamples.add(currentCPS);
            }
        }

        if (clickIntervals.isCollected()) {
            analyzeConsistency();
        }
    }

    private void analyzeConsistency() {
        double avgCPS = GrimMath.getAverage(cpsSamples);
        double stdDevCPS = GrimMath.getStandardDeviation(cpsSamples);

        double avgInterval = GrimMath.getAverageLong(clickIntervals);
        double stdDevInterval = GrimMath.getStandardDeviationLong(clickIntervals);

        if (avgInterval <= 0) return;

        double cvInterval = (stdDevInterval / avgInterval) * 100.0;
        double cvCPS = (stdDevCPS / (avgCPS > 0 ? avgCPS : 1)) * 100.0;

        if (avgCPS >= minCPSThreshold && avgCPS <= maxCPSThreshold) {

            if (cvInterval < lowVarianceThreshold) {
                lowVarianceBuffer += (lowVarianceThreshold - cvInterval) * lowVarIncrease;
            } else {
                lowVarianceBuffer = Math.max(0, lowVarianceBuffer - bufferDecrement);
            }

            if (cvInterval > consistencyThreshold && cvInterval < 25.0) {
                consistencyBuffer += (cvInterval - consistencyThreshold) * consistencyIncrease;
            } else {
                consistencyBuffer = Math.max(0, consistencyBuffer - bufferDecrement);
            }

            double combinedBuffer = consistencyBuffer + lowVarianceBuffer;

            if (combinedBuffer > bufferThreshold) {
                flagAndAlert(String.format(
                        "type=consistency cps=%.1f cv=%.1f%% low_var=%.1f cons=%.1f total=%.1f",
                        avgCPS, cvInterval, lowVarianceBuffer, consistencyBuffer, combinedBuffer
                ));

                consistencyBuffer *= 0.6;
                lowVarianceBuffer *= 0.6;

                clickIntervals.clear();
                cpsSamples.clear();
            }
        } else {
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
        this.consistencyBuffer = 0.0;
        this.lowVarianceBuffer = 0.0;
    }
}
