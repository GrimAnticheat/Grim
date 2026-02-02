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

@CheckData(name = "AutoClickerD", experimental = false, description = "Drag/Jitter click detection")
public class AutoClickerD extends Check implements PacketCheck {

    private EvictingQueue<Long> clickIntervals;
    private double cpsThreshold, stdDevThreshold, bufferIncrease, bufferDecrease, bufferTrigger;
    private double buffer = 0.0;

    public AutoClickerD(@NotNull GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.ANIMATION) return;
        if (player.packetOrderProcessor.isDigging() || player.actionManager.hasAttackedSince(500)) return;

        long interval = player.clickData.getDelay();

        if (interval >= 10 && interval <= 500) {
            clickIntervals.add(interval);
            if (clickIntervals.isCollected()) {
                analyzePattern();
                clickIntervals.clear();
            }
        }
    }

    private void analyzePattern() {
        double avgInterval = GrimMath.getAverageLong(clickIntervals);
        double stdDev = GrimMath.getStandardDeviationLong(clickIntervals);
        double cps = 1000.0 / avgInterval;

        if (cps > cpsThreshold && stdDev < stdDevThreshold) buffer += (stdDevThreshold - stdDev) * bufferIncrease;
        else buffer = Math.max(0, buffer - bufferDecrease);

        if (buffer > bufferTrigger) {
            flagAndAlert(String.format("type=drag_jitter cps=%.1f dev=%.3f", cps, stdDev));
            buffer *= 0.5;
            clickIntervals.clear();
        }
    }

    @Override
    public void onReload(ConfigManager config) {
        this.cpsThreshold = config.getDoubleElse(getConfigName() + ".cps-threshold", 14.0);
        this.stdDevThreshold = config.getDoubleElse(getConfigName() + ".stddev-threshold", 2.0);
        this.bufferIncrease = config.getDoubleElse(getConfigName() + ".buffer-increase", 1.5);
        this.bufferDecrease = config.getDoubleElse(getConfigName() + ".buffer-decrease", 0.4);
        this.bufferTrigger = config.getDoubleElse(getConfigName() + ".buffer-trigger", 8.0);
        this.clickIntervals = new EvictingQueue<>(config.getIntElse(getConfigName() + ".sample-size", 35));
    }
}
