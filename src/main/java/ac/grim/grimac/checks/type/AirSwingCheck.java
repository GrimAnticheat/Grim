package ac.grim.grimac.checks.type;

import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import java.util.LinkedList;
import java.util.Queue;

/*
 * @author Sim0n (https://github.com/sim0n/nemesis)
 */
public abstract class AirSwingCheck extends PacketCheck {
    private static final int MAX_COMBAT_TICKS = 10; // 1 minute

    private final Queue<Integer> samples = new LinkedList<>();

    private final boolean combatCheck; // if we should only check while in combat
    private final boolean doubleClicks; // if we should allow double clicks

    private final int maxSamples;

    private int movements;

    private boolean isDigging;

    public AirSwingCheck(GrimPlayer playerData, int maxSamples, boolean combatCheck, boolean doubleClicks) {
        super(playerData);

        this.maxSamples = maxSamples;

        this.combatCheck = combatCheck;
        this.doubleClicks = doubleClicks;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        // TODO: This needs to be replaced by raytracing and seeing if the ray intersects with a block. Unfortunately I don't know how to do that.
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            WrapperPlayClientPlayerDigging wrapper = new WrapperPlayClientPlayerDigging(event);
            switch (wrapper.getAction()) {
                case START_DIGGING:
                    isDigging = true;
                    break;
                case CANCELLED_DIGGING:
                case FINISHED_DIGGING:
                    isDigging = false;
                    break;
            }
        }

        if (event.getPacketType() == PacketType.Play.Client.ANIMATION && !isDigging) {
            if (movements < 10) {
                if (combatCheck && player.attackTicks > MAX_COMBAT_TICKS)
                    return;

                if (!doubleClicks && movements == 0)
                    return;

                if (player.isTickingReliablyFor(2) && samples.add(movements) && samples.size() == maxSamples) {
                    handle(samples);
                    samples.clear();
                }
            }

            movements = 0;
        } else if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            if (player.isTickingReliablyFor(2)) {
                ++movements;
            } else {
                movements = 0;
            }
        }
    }

    public abstract void handle(Queue<Integer> samples);
}
