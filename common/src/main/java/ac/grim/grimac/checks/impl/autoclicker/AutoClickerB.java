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

@CheckData(name = "AutoClickerB", experimental = false, description = "Consistency check")
public class AutoClickerB extends Check implements PacketCheck {

    private double deltaDeviationThreshold;
    private double deviationThreshold;
    private double bufferDecrement;
    private double deltaBufferTrigger;
    private double deviationBufferTrigger;
    private double minCPS;

    private EvictingQueue<Long> samples;
    private double deviationBuffer = 0;
    private double deltaDeviationBuffer = 0;
    private double lastDeviation;

    public AutoClickerB(@NotNull GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.ANIMATION) return;

        if (player.packetOrderProcessor.isDigging() || player.actionManager.hasAttackedSince(500)) return;
        if (player.clickData.getCps() < minCPS) return;

        long delay = player.clickData.getDelay();

        if (delay >= 10 && delay <= 500) {
            samples.add(delay);
        }

        if (samples.isCollected()) {
            analyzePattern();
        }
    }

    private void analyzePattern() {
        double deviation = GrimMath.getStandardDeviationLong(samples);
        double deltaDeviation = Math.abs(lastDeviation - deviation);

        if (deltaDeviation < deltaDeviationThreshold) {
            deltaDeviationBuffer += (deltaDeviationThreshold - deltaDeviation) * 10;
        } else {
            deltaDeviationBuffer = Math.max(0, deltaDeviationBuffer - bufferDecrement);
        }

        lastDeviation = deviation;

        if (deviation < deviationThreshold) {
            deviationBuffer += (deviationThreshold - deviation) * 15;
        } else {
            deviationBuffer = Math.max(0, deviationBuffer - bufferDecrement);
        }

        if (deltaDeviationBuffer > deltaBufferTrigger || deviationBuffer > deviationBufferTrigger) {
            double currentCPS = 1000.0 / GrimMath.getAverageLong(samples);

            flagAndAlert(String.format("type=consistency cps=%.1f dev=%.3f delta_dev=%.3f", currentCPS, deviation, deltaDeviation));

            deltaDeviationBuffer *= 0.5;
            deviationBuffer *= 0.5;
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
        this.samples = new EvictingQueue<>(config.getIntElse(getConfigName() + ".sample-size", 45));
    }
}
