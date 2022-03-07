package ac.grim.grimac.checks.type;

import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

public class PostCheck extends PacketCheck {
    private final PacketTypeCommon packet;
    public long lastFlying, lastPacket;
    private boolean sent = false;

    public PostCheck(final GrimPlayer playerData, final PacketTypeCommon packet) {
        super(playerData);

        this.packet = packet;
    }

    // Flag only when its both a post and a flag
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            final long now = System.currentTimeMillis();
            final long delay = now - lastPacket;

            if (sent) {
                if (delay > 40L && delay < 100L) {
                    flagAndAlert();
                } else {
                    reward();
                }

                sent = false;
            }

            this.lastFlying = now;
        } else if (event.getPacketType() == packet) {
            final long now = System.currentTimeMillis();
            final long delay = now - lastFlying;

            if (delay < 10L) {
                lastPacket = now;
                sent = true;
            } else {
                reward();
            }
        }
    }
}
