package ac.grim.grimac.checks.impl.autoclicker;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.math.GrimMath;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import org.jetbrains.annotations.NotNull;

import java.util.Deque;
import java.util.LinkedList;

@CheckData(name = "AutoClickerA", experimental = false, description = "Detects auto clickers by measuring clicks per second (CPS).")
public class AutoClickerA extends Check implements PacketCheck {

    private final Deque<Long> samples = new LinkedList<>();
    private long lastSwing = 0L;
    private double limitCps = 20.0;
    private volatile boolean breakingBlock = false;

    public AutoClickerA(@NotNull GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        // Detect block digging states to ignore mining swings
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            WrapperPlayClientPlayerDigging dig = new WrapperPlayClientPlayerDigging(event);
            switch (dig.getAction()) {
                case START_DIGGING -> breakingBlock = true;
                case FINISHED_DIGGING, CANCELLED_DIGGING, DROP_ITEM, DROP_ITEM_STACK, SWAP_ITEM_WITH_OFFHAND -> breakingBlock = false;
            }
            return;
        }

        // Handle swing (click) packets
        if (event.getPacketType() == PacketType.Play.Client.ANIMATION && !isExemptClick()) {
            if (breakingBlock) return; // ignore while mining

            final long now = System.currentTimeMillis();
            final long delay = now - lastSwing;

            // Ignore invalid or long gaps (holding click, idle, etc.)
            if (delay <= 0L || delay > 250L) {
                lastSwing = now;
                return;
            }

            samples.add(delay);
            if (samples.size() >= 20) {
                double cps = GrimMath.getCps(samples);

                if (cps > limitCps) {
                    flagAndAlert("cps=" + cps);
                }

                samples.clear();
            }
            lastSwing = now;
        }
    }

    @Override
    public void onReload(ConfigManager config) {
        // Max CPS limit (default 20)
        limitCps = config.getDoubleElse(getConfigName() + ".max-cps", 20.0);
    }

    /**
     * Returns true if the click should be ignored (e.g., while breaking blocks)
     */
    public boolean isExemptClick() {
        return breakingBlock;
    }
}
