package ac.grim.grimac.checks.impl.movement;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

import java.util.ArrayDeque;
import java.util.Deque;

public class FakeLag extends Check implements PacketCheck {

    private static final int MaxPackets = 3;
    private static final int Interval = 50;


    private final Deque<Long> MovementPackets = new ArrayDeque<>();

    public FakeLag(GrimPlayer playerData) {
        super(playerData);
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (player.disableGrim) return;

        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            long now = System.currentTimeMillis();
            MovementPackets.addLast(now);

            while (!MovementPackets.isEmpty() && now - MovementPackets.peekFirst() > Interval) {
                MovementPackets.pollFirst();
            }
            // Detects and prevents FakeLag abuse effectively.
            // May occasionally cause setbacks for legitimately lagging players, unavoidable due to widespread exploitation.

            if (MovementPackets.size() > MaxPackets) {
                // Apply a setback to players sending more than 3 packets per tick,
                // as normal players should not exceed this rate.

                flagWithSetback();
                MovementPackets.clear();
            }
        }
    }
}
