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

@CheckData(name = "AutoClickerF", experimental = true, description = "Detects modified interval patterns and click timing anomalies")
public class AutoClickerF extends Check implements PacketCheck {

    private EvictingQueue<Long> intervals;
    private EvictingQueue<Double> deviationRatios;
    
    private long lastSwingTime = -1L;
    private boolean digging = false;

    // Configuration
    private int sampleSize;
    private double minCPS;
    private double maxCPS;
    private double deviationThreshold;
    private double patternSimilarityThreshold;
    private double repetitionThreshold;
    private double bufferIncrease;
    private double bufferDecrease;
    private double bufferLimit;

    // Buffers and counters
    private double deviationBuffer = 0.0;
    private double patternBuffer = 0.0;
    private double repetitionBuffer = 0.0;

    public AutoClickerF(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        // Detect if the player is digging blocks
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
            
            if (interval >= 25 && interval <= 500) { // Stricter range for better detection
                intervals.add(interval);
                
                // Calculate instant deviation ratio
                if (intervals.size() >= 2) {
                    double currentDeviation = calculateInstantDeviationRatio();
                    deviationRatios.add(currentDeviation);
                }
            }
        }
        lastSwingTime = now;

        if (intervals.isCollected()) {
            analyzeModifiedPatterns();
        }
    }

    private double calculateInstantDeviationRatio() {
        int size = intervals.size();
        if (size < 2) return 0.0;
        
        long current = intervals.get(size - 1);
        long previous = intervals.get(size - 2);
        
        if (previous == 0) return 0.0;
        
        double ratio = (double) Math.abs(current - previous) / previous;
        return ratio * 100; // Convert to percentage
    }

    private void analyzeModifiedPatterns() {
        double avgCPS = 1000.0 / GrimMath.getAverageLong(intervals);
        
        // Analyze only within CPS range
        if (avgCPS < minCPS || avgCPS > maxCPS) {
            deviationBuffer = Math.max(0, deviationBuffer - bufferDecrease);
            patternBuffer = Math.max(0, patternBuffer - bufferDecrease);
            repetitionBuffer = Math.max(0, repetitionBuffer - bufferDecrease);
            return;
        }

        // 1. Artificial Deviation Detection
        double avgDeviationRatio = GrimMath.getAverage(deviationRatios);
        if (avgDeviationRatio < deviationThreshold) {
            deviationBuffer += (deviationThreshold - avgDeviationRatio) * bufferIncrease;
        } else {
            deviationBuffer = Math.max(0, deviationBuffer - bufferDecrease);
        }

        // 2. Pattern Similarity Detection
        double patternScore = analyzePatternSimilarity();
        if (patternScore > patternSimilarityThreshold) {
            patternBuffer += (patternScore - patternSimilarityThreshold) * bufferIncrease;
        } else {
            patternBuffer = Math.max(0, patternBuffer - bufferDecrease);
        }

        // 3. Interval Repetition Detection
        double repetitionScore = analyzeIntervalRepetition();
        if (repetitionScore > repetitionThreshold) {
            repetitionBuffer += (repetitionScore - repetitionThreshold) * bufferIncrease;
        } else {
            repetitionBuffer = Math.max(0, repetitionBuffer - bufferDecrease);
        }

        // Combine detections
        double totalBuffer = deviationBuffer + patternBuffer + repetitionBuffer;
        
        if (totalBuffer > bufferLimit) {
            flagAndAlert(String.format(
                "type=modified dev=%.1f pat=%.1f rep=%.1f cps=%.1f dev_ratio=%.1f%% total=%.1f",
                deviationBuffer, patternBuffer, repetitionBuffer, avgCPS, avgDeviationRatio, totalBuffer
            ));
            
            // Soft reset of buffers
            deviationBuffer *= 0.6;
            patternBuffer *= 0.6;
            repetitionBuffer *= 0.6;
            
            // Clear some samples to avoid constant detection
            if (intervals.size() > sampleSize / 2) {
                intervals.clear();
                deviationRatios.clear();
            }
        }
    }

    private double analyzePatternSimilarity() {
        if (intervals.size() < 6) return 0.0;
        
        int similarPatterns = 0;
        int totalComparisons = 0;
        
        // Compare sequences of 3 intervals
        for (int i = 0; i < intervals.size() - 3; i++) {
            long[] pattern1 = {
                intervals.get(i),
                intervals.get(i + 1), 
                intervals.get(i + 2)
            };
            
            for (int j = i + 3; j < intervals.size() - 3; j++) {
                long[] pattern2 = {
                    intervals.get(j),
                    intervals.get(j + 1),
                    intervals.get(j + 2)
                };
                
                if (arePatternsSimilar(pattern1, pattern2)) {
                    similarPatterns++;
                }
                totalComparisons++;
            }
        }
        
        return totalComparisons > 0 ? (double) similarPatterns / totalComparisons * 100 : 0.0;
    }

    private boolean arePatternsSimilar(long[] pattern1, long[] pattern2) {
        double tolerance = 0.15; // 15% tolerance
        
        for (int i = 0; i < pattern1.length; i++) {
            double diff = Math.abs(pattern1[i] - pattern2[i]);
            double avg = (pattern1[i] + pattern2[i]) / 2.0;
            
            if (avg > 0 && diff / avg > tolerance) {
                return false;
            }
        }
        return true;
    }

    private double analyzeIntervalRepetition() {
        if (intervals.size() < 4) return 0.0;
        
        int repetitions = 0;
        
        // Look for identical or very similar intervals
        for (int i = 0; i < intervals.size() - 1; i++) {
            long current = intervals.get(i);
            
            for (int j = i + 1; j < intervals.size(); j++) {
                long other = intervals.get(j);
                double diff = Math.abs(current - other);
                
                if (diff <= 2) { // 2ms difference or less
                    repetitions++;
                }
            }
        }
        
        // Normalize based on possible comparisons
        int maxComparisons = intervals.size() * (intervals.size() - 1) / 2;
        return maxComparisons > 0 ? (double) repetitions / maxComparisons * 100 : 0.0;
    }

    @Override
    public void onReload(ConfigManager config) {
        this.sampleSize = config.getIntElse(getConfigName() + ".sample-size", 30);
        this.minCPS = config.getDoubleElse(getConfigName() + ".min-cps", 7.0);
        this.maxCPS = config.getDoubleElse(getConfigName() + ".max-cps", 18.0);
        this.deviationThreshold = config.getDoubleElse(getConfigName() + ".deviation-threshold", 12.0);
        this.patternSimilarityThreshold = config.getDoubleElse(getConfigName() + ".pattern-threshold", 25.0);
        this.repetitionThreshold = config.getDoubleElse(getConfigName() + ".repetition-threshold", 15.0);
        this.bufferIncrease = config.getDoubleElse(getConfigName() + ".buffer-increase", 0.2);
        this.bufferDecrease = config.getDoubleElse(getConfigName() + ".buffer-decrease", 0.12);
        this.bufferLimit = config.getDoubleElse(getConfigName() + ".buffer-limit", 5.0);

        this.intervals = new EvictingQueue<>(sampleSize);
        this.deviationRatios = new EvictingQueue<>(sampleSize - 1);
        this.lastSwingTime = -1L;
        this.deviationBuffer = 0.0;
        this.patternBuffer = 0.0;
        this.repetitionBuffer = 0.0;
        this.digging = false;
    }
}
