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

@CheckData(name = "AutoClickerC", experimental = true, description = "Unstable high CPS")
public class AutoClickerC extends Check implements PacketCheck {

    private EvictingQueue<Double> cpsSamples;
    private double deviationBuffer = 0.0;
    private double spikeBuffer = 0.0;
    private double cpsThreshold, unstableThreshold, spikeMultiplier, deviationBufferTrigger, spikeBufferTrigger, bufferDecrement, spikeBufferDecrement;

    public AutoClickerC(@NotNull GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.ANIMATION) return;
        if (player.packetOrderProcessor.isDigging() || player.actionManager.hasAttackedSince(500)) return;

        double currentCps = player.clickData.getCps();
        if (currentCps > 0) cpsSamples.add(currentCps);

        if (cpsSamples.isCollected()) analyzePattern();
    }

    private void analyzePattern() {
        double avg = GrimMath.getAverage(cpsSamples);
        double stdDev = GrimMath.getStandardDeviation(cpsSamples);
        double max = 0.0;
        for (double v : cpsSamples) if (v > max) max = v;

        if (avg > cpsThreshold && stdDev > unstableThreshold) deviationBuffer += (stdDev - unstableThreshold) * 0.5;
        else deviationBuffer = Math.max(0.0, deviationBuffer - bufferDecrement);

        if (avg > 0 && max / avg >= spikeMultiplier && avg > cpsThreshold) spikeBuffer += (max / avg - spikeMultiplier) * 0.8;
        else spikeBuffer = Math.max(0.0, spikeBuffer - spikeBufferDecrement);

        if (deviationBuffer > deviationBufferTrigger && spikeBuffer > spikeBufferTrigger) {
            flagAndAlert(String.format("type=unstable_spike cps=%.1f dev=%.2f", avg, stdDev));
            deviationBuffer *= 0.6; spikeBuffer *= 0.6; cpsSamples.clear();
        } else if (deviationBuffer > deviationBufferTrigger * 1.5) {
            flagAndAlert(String.format("type=high_deviation cps=%.1f dev=%.2f", avg, stdDev));
            deviationBuffer *= 0.7; cpsSamples.clear();
        }
    }

    @Override
    public void onReload(ConfigManager config) {
        this.cpsThreshold = config.getDoubleElse(getConfigName() + ".cps-threshold", 11.0);
        this.unstableThreshold = config.getDoubleElse(getConfigName() + ".unstable-threshold", 4.0);
        this.spikeMultiplier = config.getDoubleElse(getConfigName() + ".spike-multiplier", 2.0);
        this.deviationBufferTrigger = config.getDoubleElse(getConfigName() + ".deviation-buffer-trigger", 6.0);
        this.spikeBufferTrigger = config.getDoubleElse(getConfigName() + ".spike-buffer-trigger", 1.5);
        this.bufferDecrement = config.getDoubleElse(getConfigName() + ".buffer-decrement", 0.25);
        this.spikeBufferDecrement = config.getDoubleElse(getConfigName() + ".spike-buffer-decrement", 0.3);
        this.cpsSamples = new EvictingQueue<>(config.getIntElse(getConfigName() + ".cps-sample-size", 40));
    }
}
