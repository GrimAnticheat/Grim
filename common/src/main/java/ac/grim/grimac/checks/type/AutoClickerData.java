package ac.grim.grimac.utils.data;

import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.util.EvictingQueue;
import lombok.Getter;

import java.util.Queue;

/*
Author: RizeDev
*/

@Getter
public class AutoClickerData {
    private final GrimPlayer player;

    // Basic click data
    private long lastClickTime = System.currentTimeMillis();
    private final EvictingQueue<Long> clickIntervals = EvictingQueue.create(20); // Last 20 intervals
    private final EvictingQueue<Double> cpsSamples = EvictingQueue.create(10);  // CPS samples
    private int clickCount = 0;
    private long lastCpsCalculation = System.currentTimeMillis();

    // Patterns and statistics
    private final EvictingQueue<Integer> outliers = EvictingQueue.create(10);
    private double averageCps = 0.0;
    private double cpsDeviation = 0.0;
    private double averageInterval = 0.0;
    private double intervalDeviation = 0.0;

    // Flags and states
    private boolean isBreakingBlock = false;
    private boolean isPlacingBlock = false;
    private boolean isEating = false;
    private boolean isUsingItem = false;
    private long lastBlockBreak = 0;
    private long lastBlockPlace = 0;

    // Click distribution
    private int leftClicks = 0;
    private int rightClicks = 0;
    private final EvictingQueue<Boolean> clickSequence = EvictingQueue.create(50);

    public AutoClickerData(GrimPlayer player) {
        this.player = player;
    }

    /**
     * Records a player click
     */
    public void recordClick(boolean isLeftClick) {
        long currentTime = System.currentTimeMillis();
        long timeSinceLastClick = currentTime - lastClickTime;

        // Only register valid intervals between 10ms and 2000ms
        if (timeSinceLastClick >= 10 && timeSinceLastClick <= 2000) {
            clickIntervals.add(timeSinceLastClick);
            updateStatistics();
        }

        // Update counters
        if (isLeftClick) {
            leftClicks++;
            clickSequence.add(true);
        } else {
            rightClicks++;
            clickSequence.add(false);
        }

        clickCount++;
        lastClickTime = currentTime;

        // CPS every second
        if (currentTime - lastCpsCalculation >= 1000) {
            calculateCPS();
            lastCpsCalculation = currentTime;
        }
    }

    /**
     * Calculates and updates CPS statistics.
     */
    private void calculateCPS() {
        long currentTime = System.currentTimeMillis();

        while (!clickIntervals.isEmpty()) {
            long interval = clickIntervals.peek();
            if (currentTime - (interval * clickIntervals.size()) > 1000) {
                clickIntervals.poll();
            } else break;
        }

        double currentCps = clickIntervals.size();
        cpsSamples.add(currentCps);

        if (cpsSamples.size() >= 3) {
            averageCps = calculateAverage(cpsSamples);
            cpsDeviation = calculateStandardDeviation(cpsSamples, averageCps);
        }

        clickCount = 0;
    }

    /**
     * Updates interval statistics.
     */
    private void updateStatistics() {
        if (clickIntervals.size() >= 5) {
            averageInterval = calculateAverageLong(clickIntervals);
            intervalDeviation = calculateStandardDeviationLong(clickIntervals, averageInterval);

            detectOutliers();
        }
    }

    /**
     * Detect outlier intervals
     */
    private void detectOutliers() {
        if (clickIntervals.size() < 5) return;

        double threshold = intervalDeviation * 2.0;

        for (Long interval : clickIntervals) {
            if (Math.abs(interval - averageInterval) > threshold) {
                outliers.add(interval.intValue());
            }
        }

        while (outliers.size() > 10) {
            outliers.poll();
        }
    }

    public boolean isConsistentPattern() {
        if (clickIntervals.size() < 8) return false;

        double threshold = averageInterval * 0.15;

        for (Long interval : clickIntervals) {
            if (Math.abs(interval - averageInterval) > threshold) {
                return false;
            }
        }
        return true;
    }

    public boolean hasRepeatingPattern() {
        if (clickIntervals.size() < 10) return false;

        Long[] intervals = clickIntervals.toArray(new Long[0]);

        for (int size = 2; size <= 4; size++) {
            if (hasPattern(intervals, size)) return true;
        }

        return false;
    }

    private boolean hasPattern(Long[] intervals, int patternSize) {
        if (intervals.length < patternSize * 2) return false;

        for (int i = 0; i <= intervals.length - patternSize * 2; i++) {
            boolean match = true;

            for (int j = 0; j < patternSize; j++) {
                double diff = Math.abs(intervals[i + j] - intervals[i + j + patternSize]);
                if (diff > (intervals[i + j] * 0.1)) {
                    match = false;
                    break;
                }
            }

            if (match) return true;
        }

        return false;
    }

    public double getCurrentCPS() {
        return clickIntervals.size();
    }

    public boolean isExempt() {
        long now = System.currentTimeMillis();
        return isBreakingBlock ||
               isPlacingBlock ||
               isEating ||
               isUsingItem ||
               now - lastBlockBreak < 100 ||
               now - lastBlockPlace < 100;
    }

    public void setBreakingBlock(boolean breaking) {
        this.isBreakingBlock = breaking;
        if (breaking) lastBlockBreak = System.currentTimeMillis();
    }

    public void setPlacingBlock(boolean placing) {
        this.isPlacingBlock = placing;
        if (placing) lastBlockPlace = System.currentTimeMillis();
    }

    public void setEating(boolean eating) {
        this.isEating = eating;
    }

    public void setUsingItem(boolean using) {
        this.isUsingItem = using;
    }

    public void reset() {
        clickIntervals.clear();
        cpsSamples.clear();
        outliers.clear();
        clickSequence.clear();
        clickCount = 0;
        averageCps = 0.0;
        cpsDeviation = 0.0;
        averageInterval = 0.0;
        intervalDeviation = 0.0;
        leftClicks = 0;
        rightClicks = 0;
    }

    // Utility methods
    private double calculateAverage(Queue<Double> numbers) {
        return numbers.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    private double calculateAverageLong(Queue<Long> numbers) {
        return numbers.stream().mapToLong(Long::longValue).average().orElse(0.0);
    }

    private double calculateStandardDeviation(Queue<Double> numbers, double avg) {
        if (numbers.size() < 2) return 0.0;
        double sum = numbers.stream().mapToDouble(n -> Math.pow(n - avg, 2)).sum();
        return Math.sqrt(sum / numbers.size());
    }

    private double calculateStandardDeviationLong(Queue<Long> numbers, double avg) {
        if (numbers.size() < 2) return 0.0;
        double sum = numbers.stream().mapToDouble(n -> Math.pow(n - avg, 2)).sum();
        return Math.sqrt(sum / numbers.size());
    }
}
