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

@CheckData(name = "AutoClickerG", experimental = false, description = "Detects burst patterns and temporal distribution anomalies")
public class AutoClickerG extends Check implements PacketCheck {

    private EvictingQueue<Long> intervals;
    private EvictingQueue<Integer> burstLengths;
    private EvictingQueue<Double> distributionScores;

    private long lastSwingTime = -1L;
    private int currentBurstCount = 0;
    private long burstStartTime = -1L;
    private boolean digging = false;

    // Configuration
    private int sampleSize;
    private double minBurstCPS;
    private double maxAllowedBurstLength;
    private double distributionAnomalyThreshold;
    private double burstFrequencyThreshold;
    private double burstBufferIncrease;
    private double distributionBufferIncrease;
    private double frequencyBufferIncrease;
    private double bufferDecrease;
    private double bufferLimit;

    // Detection buffers
    private double burstBuffer = 0.0;
    private double distributionBuffer = 0.0;
    private double frequencyBuffer = 0.0;

    public AutoClickerG(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        // Detect if the player is mining blocks
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            WrapperPlayClientPlayerDigging dig = new WrapperPlayClientPlayerDigging(event);
            switch (dig.getAction()) {
                case START_DIGGING -> digging = true;
                case CANCELLED_DIGGING, FINISHED_DIGGING, DROP_ITEM_STACK, DROP_ITEM, SWAP_ITEM_WITH_OFFHAND -> digging = false;
            }
            return;
        }

        // Ignore during mining or recent attacks
        if (event.getPacketType() != PacketType.Play.Client.ANIMATION) return;
        if (digging || player.actionManager.hasAttackedSince(500)) return;

        long now = System.currentTimeMillis();

        if (lastSwingTime != -1L) {
            long interval = now - lastSwingTime;

            if (interval >= 20 && interval <= 1000) {
                intervals.add(interval);

                // Burst clicking analysis
                analyzeBurstPattern(interval, now);

                // Temporal distribution analysis
                if (intervals.size() >= 10) {
                    analyzeTemporalDistribution();
                }
            }
        }
        lastSwingTime = now;

        if (intervals.isCollected()) {
            performAdvancedAnalysis();
        }
    }

    private void analyzeBurstPattern(long interval, long currentTime) {
        // Detect burst start (consecutive short intervals)
        if (interval <= 100) { // Less than 100ms between clicks
            if (currentBurstCount == 0) {
                burstStartTime = currentTime;
            }
            currentBurstCount++;
        } else {
            // Burst ended
            if (currentBurstCount > 0) {
                long burstDuration = currentTime - burstStartTime;
                double burstCPS = (currentBurstCount * 1000.0) / Math.max(1, burstDuration);

                if (burstCPS >= minBurstCPS && currentBurstCount >= 3) {
                    burstLengths.add(currentBurstCount);
                }

                currentBurstCount = 0;
                burstStartTime = -1L;
            }
        }
    }

    private void analyzeTemporalDistribution() {
        // Measure how uniformly spaced the clicks are over time
        double[] normalizedIntervals = new double[intervals.size()];
        double sum = 0;

        for (int i = 0; i < intervals.size(); i++) {
            sum += intervals.get(i);
        }
        double mean = sum / intervals.size();

        // Normalize intervals
        for (int i = 0; i < intervals.size(); i++) {
            normalizedIntervals[i] = intervals.get(i) / mean;
        }

        // Calculate deviation from randomness
        double distributionScore = calculateDistributionScore(normalizedIntervals);
        distributionScores.add(distributionScore);
    }

    private double calculateDistributionScore(double[] normalizedIntervals) {
        // Human clicking is more random. Autoclickers have more predictable patterns.

        double runs = 1; // Count "runs" — changes between high/low values
        double lastValue = normalizedIntervals[0];

        for (int i = 1; i < normalizedIntervals.length; i++) {
            if ((normalizedIntervals[i] > 1.0 && lastValue <= 1.0) ||
                (normalizedIntervals[i] <= 1.0 && lastValue > 1.0)) {
                runs++;
            }
            lastValue = normalizedIntervals[i];
        }

        // Wald-Wolfowitz runs test for randomness
        double n = normalizedIntervals.length;
        double expectedRuns = (2 * n - 1) / 3.0;
        double variance = (16 * n - 29) / 90.0;

        if (variance <= 0) return 0.0;

        double zScore = Math.abs((runs - expectedRuns) / Math.sqrt(variance));
        return zScore;
    }

    private void performAdvancedAnalysis() {
        // 1. Abnormal burst detection
        if (!burstLengths.isEmpty()) {
            double avgBurstLength = GrimMath.getAverageInt(burstLengths);
            if (avgBurstLength > maxAllowedBurstLength) {
                burstBuffer += (avgBurstLength - maxAllowedBurstLength) * burstBufferIncrease;
            }
        }

        burstBuffer = Math.max(0, burstBuffer - bufferDecrease);

        // 2. Temporal distribution anomalies
        if (!distributionScores.isEmpty()) {
            double avgDistributionScore = GrimMath.getAverage(distributionScores);
            if (avgDistributionScore < distributionAnomalyThreshold) {
                distributionBuffer += (distributionAnomalyThreshold - avgDistributionScore) * distributionBufferIncrease;
            }
        }

        distributionBuffer = Math.max(0, distributionBuffer - bufferDecrease);

        // 3. Burst frequency detection
        if (!burstLengths.isEmpty()) {
            double burstFrequency = (double) burstLengths.size() / intervals.size() * 100;
            if (burstFrequency > burstFrequencyThreshold) {
                frequencyBuffer += (burstFrequency - burstFrequencyThreshold) * frequencyBufferIncrease;
            }
        }

        frequencyBuffer = Math.max(0, frequencyBuffer - bufferDecrease);

        // Combine all detections
        double totalBuffer = burstBuffer + distributionBuffer + frequencyBuffer;

        if (totalBuffer > bufferLimit) {
            double currentCPS = 1000.0 / GrimMath.getAverageLong(intervals);

            flagAndAlert(String.format(
                "type=burst_distribution cps=%.1f bursts=%.1f dist=%.1f freq=%.1f total=%.1f",
                currentCPS, burstBuffer, distributionBuffer, frequencyBuffer, totalBuffer
            ));

            // Soft reset
            burstBuffer *= 0.6;
            distributionBuffer *= 0.6;
            frequencyBuffer *= 0.6;

            // Clear some samples but keep history
            if (intervals.size() > sampleSize / 2) {
                intervals.clear();
                burstLengths.clear();
                distributionScores.clear();
            }
        }
    }

    @Override
    public void onReload(ConfigManager config) {
        this.sampleSize = config.getIntElse(getConfigName() + ".sample-size", 40);
        this.minBurstCPS = config.getDoubleElse(getConfigName() + ".min-burst-cps", 12.0);
        this.maxAllowedBurstLength = config.getDoubleElse(getConfigName() + ".max-burst-length", 5.0);
        this.distributionAnomalyThreshold = config.getDoubleElse(getConfigName() + ".distribution-threshold", 1.5);
        this.burstFrequencyThreshold = config.getDoubleElse(getConfigName() + ".burst-frequency-threshold", 35.0);
        this.burstBufferIncrease = config.getDoubleElse(getConfigName() + ".burst-buffer-increase", 0.3);
        this.distributionBufferIncrease = config.getDoubleElse(getConfigName() + ".distribution-buffer-increase", 0.4);
        this.frequencyBufferIncrease = config.getDoubleElse(getConfigName() + ".frequency-buffer-increase", 0.2);
        this.bufferDecrease = config.getDoubleElse(getConfigName() + ".buffer-decrease", 0.1);
        this.bufferLimit = config.getDoubleElse(getConfigName() + ".buffer-limit", 6.0);

        this.intervals = new EvictingQueue<>(sampleSize);
        this.burstLengths = new EvictingQueue<>(sampleSize / 2);
        this.distributionScores = new EvictingQueue<>(sampleSize / 2);
        this.lastSwingTime = -1L;
        this.currentBurstCount = 0;
        this.burstStartTime = -1L;
        this.burstBuffer = 0.0;
        this.distributionBuffer = 0.0;
        this.frequencyBuffer = 0.0;
        this.digging = false;
    }
}
