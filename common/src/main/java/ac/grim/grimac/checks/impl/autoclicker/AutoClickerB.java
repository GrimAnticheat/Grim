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

@CheckData(name = "AutoClickerB", experimental = false, description = "Detects consistent clicking patterns.")
public class AutoClickerB extends Check implements PacketCheck {

    private double deltaDeviationThreshold;
    private double deviationThreshold;
    private double bufferDecrement;
    private double deltaBufferTrigger;
    private double deviationBufferTrigger;
    private double minCPS;

    private EvictingQueue<Integer> samples;
    private int lastClickTick;
    private double deviationBuffer = 0;
    private double deltaDeviationBuffer = 0;
    private double lastDeviation;
    private boolean digging = false;

    public AutoClickerB(@NotNull GrimPlayer player) {
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

        // Only process animation packets (clicks)
        if (event.getPacketType() != PacketType.Play.Client.ANIMATION) return;

        // Ignore clicks during mining or recent attacks
        boolean breakingBlock = digging || player.actionManager.hasAttackedSince(500);
        boolean notEnoughCPS = player.clickData.getCps() < minCPS;

        if (breakingBlock || notEnoughCPS) {
            return;
        }

        int delta = player.totalFlyingPacketsSent - lastClickTick;
        lastClickTick = player.totalFlyingPacketsSent;

        // Only add reasonable deltas (between 1 and 20 ticks)
        if (delta >= 1 && delta <= 20) {
            samples.add(delta);
        }

        if (samples.isCollected()) {
            analyzePattern();
        }
    }

    private void analyzePattern() {
        double deviation = GrimMath.getStandardDeviationInt(samples);
        double deltaDeviation = Math.abs(lastDeviation - deviation);

        // 1. Delta Deviation Detection (consistency between samples)
        if (deltaDeviation < deltaDeviationThreshold) {
            deltaDeviationBuffer += (deltaDeviationThreshold - deltaDeviation) * 10;
        } else {
            deltaDeviationBuffer = Math.max(0, deltaDeviationBuffer - bufferDecrement);
        }

        lastDeviation = deviation;

        // 2. Low Deviation Detection (highly consistent patterns)
        if (deviation < deviationThreshold) {
            deviationBuffer += (deviationThreshold - deviation) * 15;
        } else {
            deviationBuffer = Math.max(0, deviationBuffer - bufferDecrement);
        }

        // 3. Combine detections and flag
        if (deltaDeviationBuffer > deltaBufferTrigger || deviationBuffer > deviationBufferTrigger) {
            double currentCPS = 1000.0 / (GrimMath.getAverageInt(samples) * 50.0);

            flagAndAlert(String.format(
                "type=consistency cps=%.1f dev=%.3f delta_dev=%.3f dev_buf=%.1f delta_buf=%.1f",
                currentCPS, deviation, deltaDeviation, deviationBuffer, deltaDeviationBuffer
            ));

            // Partial buffer reset after flagging
            if (deltaDeviationBuffer > deltaBufferTrigger) {
                deltaDeviationBuffer *= 0.5;
            }
            if (deviationBuffer > deviationBufferTrigger) {
                deviationBuffer *= 0.5;
            }

            // Clear samples to avoid repeated detections
            samples.clear();
        }
    }

    @Override
    public void onReload(ConfigManager config) {
        this.deltaDeviationThreshold = config.getDoubleElse(getConfigName() + ".delta-deviation-threshold", 0.015);
        this.deviationThreshold = config.getDoubleElse(getConfigName() + ".deviation-threshold", 0.8);
        this.bufferDecrement = config.getDoubleElse(getConfigName() + ".buffer-decrement", 0.25);
        this.deltaBufferTrigger = config.getDoubleElse(getConfigName() + ".delta-buffer-trigger", 3.0);
        this.deviationBufferTrigger = config.getDoubleElse(getConfigName() + ".deviation-buffer-trigger", 8.0);
        this.minCPS = config.getDoubleElse(getConfigName() + ".min-cps", 9.0);

        int sampleSize = config.getIntElse(getConfigName() + ".sample-size", 45);
        this.samples = new EvictingQueue<>(sampleSize);
        this.lastClickTick = 0;
        this.deviationBuffer = 0.0;
        this.deltaDeviationBuffer = 0.0;
        this.lastDeviation = 0.0;
        this.digging = false;
    }
}
