package ac.grim.grimac.utils.data;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.math.GrimMath;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;

public class ClickData
{
    private final GrimPlayer data;
    private long lastSwing;
    private long delay;
    private long lastInteractEntity;
    private final SampleList<Long> samples;
    private double cps;
    private double kurtosis;

    public ClickData(final GrimPlayer data) {
        this.lastSwing = -1L;
        this.lastInteractEntity = System.currentTimeMillis();
        this.samples = new SampleList<>(20);
        this.data = data;
    }

    public void handleArmAnimation(PacketTypeCommon packetType) {
        if (packetType == PacketType.Play.Client.ANIMATION && !(data.packetOrderProcessor.isClickingInInventory() || data.packetOrderProcessor.isDigging() || data.packetOrderProcessor.isPlacing())) {
            final long now = System.currentTimeMillis();
            if (this.lastSwing > 0L) {
                this.delay = now - this.lastSwing;
                this.samples.add(this.delay);
                if (this.samples.isCollected()) {
                    this.cps = GrimMath.getCps(this.samples);
                }
            }
            this.lastSwing = now;
        }
    }

    public GrimPlayer getData() {
        return this.data;
    }

    public long getLastSwing() {
        return this.lastSwing;
    }

    public long getDelay() {
        return this.delay;
    }

    public long getLastInteractEntity() {
        return this.lastInteractEntity;
    }

    public SampleList<Long> getSamples() {
        return this.samples;
    }

    public double getCps() {
        return this.cps;
    }

    public double getKurtosis() {
        return this.kurtosis;
    }

    public void setLastSwing(final long lastSwing) {
        this.lastSwing = lastSwing;
    }

    public void setDelay(final long delay) {
        this.delay = delay;
    }

    public void setLastInteractEntity(final long lastInteractEntity) {
        this.lastInteractEntity = lastInteractEntity;
    }

    public void setCps(final double cps) {
        this.cps = cps;
    }

    public void setKurtosis(final double kurtosis) {
        this.kurtosis = kurtosis;
    }
}
