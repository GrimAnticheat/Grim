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

@CheckData(name = "AutoClickerC", experimental = true, description = "Detects unstable high CPS intervals")
public class AutoClickerC extends Check implements PacketCheck {

    private EvictingQueue<Double> cpsSamples;
    private EvictingQueue<Integer> tickDeltas;

    private long lastSwingTime = -1L;
    private int lastClickTick = 0;

    private double deviationBuffer = 0.0;
    private double spikeBuffer = 0.0;

    private double cpsThreshold;
    private double unstableThreshold;
    private double spikeMultiplier;
    private int cpsSampleSize;
    private int tickSampleSize;
    private double deviationBufferTrigger;
    private double spikeBufferTrigger;
    private double bufferDecrement;
    private double spikeBufferDecrement;

    private boolean digging = false;

    public AutoClickerC(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        // Detect if the player is breaking blocks
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
            long delay = now - lastSwingTime;
            // Only process reasonable intervals (20ms to 1000ms)
            if (delay >= 20 && delay <= 1000) {
                double cps = 1000.0 / delay;
                cpsSamples.add(cps);
            }
        }
        lastSwingTime = now;

        int currentTick = player.totalFlyingPacketsSent;
        int delta = currentTick - lastClickTick;
        if (lastClickTick != 0 && delta >= 1 && delta <= 20) {
            tickDeltas.add(delta);
        }
        lastClickTick = currentTick;

        if (cpsSamples.isCollected()) analyzePattern();
    }

    private void analyzePattern() {
        double avg = GrimMath.getAverage(cpsSamples);
        double stdDev = GrimMath.getStandardDeviation(cpsSamples);

        double max = 0.0;
        for (double v : cpsSamples) {
            if (v > max) max = v;
        }

        // 1. Detect unstable CPS deviation (randomness too high)
        if (avg > cpsThreshold && stdDev > unstableThreshold) {
            deviationBuffer += (stdDev - unstableThreshold) * 0.5;
        } else {
            deviationBuffer = Math.max(0.0, deviationBuffer - bufferDecrement);
        }

        // 2. Detect clicking spikes (max CPS much higher than average)
        if (avg > 0 && max / avg >= spikeMultiplier && avg > cpsThreshold) {
            spikeBuffer += (max / avg - spikeMultiplier) * 0.8;
        } else {
            spikeBuffer = Math.max(0.0, spikeBuffer - spikeBufferDecrement);
        }

        // 3. Trigger detection with combined conditions
        if (deviationBuffer > deviationBufferTrigger && spikeBuffer > spikeBufferTrigger) {
            flagAndAlert(String.format(
                "type=unstable_spike cps=%.1f max=%.1f dev=%.2f dev_buf=%.1f spike_buf=%.1f",
                avg, max, stdDev, deviationBuffer, spikeBuffer
            ));

            // Soft reset buffers after detection
            deviationBuffer *= 0.6;
            spikeBuffer *= 0.6;

            // Clear samples to avoid repeated detections on same pattern
            cpsSamples.clear();
            tickDeltas.clear();
        }

        // Additional detection: Very high deviation alone
        else if (deviationBuffer > deviationBufferTrigger * 1.5) {
            flagAndAlert(String.format(
                "type=high_deviation cps=%.1f dev=%.2f dev_buf=%.1f",
                avg, stdDev, deviationBuffer
            ));
            deviationBuffer *= 0.7;
            cpsSamples.clear();
        }
    }

    @Override
    public void onReload(ConfigManager config) {
        this.cpsSampleSize = config.getIntElse(getConfigName() + ".cps-sample-size", 40);
        this.tickSampleSize = config.getIntElse(getConfigName() + ".tick-sample-size", 50);
        this.cpsThreshold = config.getDoubleElse(getConfigName() + ".cps-threshold", 11.0);
        this.unstableThreshold = config.getDoubleElse(getConfigName() + ".unstable-threshold", 4.0);
        this.spikeMultiplier = config.getDoubleElse(getConfigName() + ".spike-multiplier", 2.0);
        this.deviationBufferTrigger = config.getDoubleElse(getConfigName() + ".deviation-buffer-trigger", 6.0);
        this.spikeBufferTrigger = config.getDoubleElse(getConfigName() + ".spike-buffer-trigger", 1.5);
        this.bufferDecrement = config.getDoubleElse(getConfigName() + ".buffer-decrement", 0.25);
        this.spikeBufferDecrement = config.getDoubleElse(getConfigName() + ".spike-buffer-decrement", 0.3);

        this.cpsSamples = new EvictingQueue<>(cpsSampleSize);
        this.tickDeltas = new EvictingQueue<>(tickSampleSize);
        this.lastSwingTime = -1L;
        this.lastClickTick = 0;
        this.deviationBuffer = 0.0;
        this.spikeBuffer = 0.0;
        this.digging = false;
    }
}
