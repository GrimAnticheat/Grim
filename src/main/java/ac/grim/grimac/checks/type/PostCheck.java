package ac.grim.grimac.checks.type;

import ac.grim.grimac.player.GrimPlayer;
import io.github.retrooper.packetevents.event.impl.PacketPlayReceiveEvent;
import io.github.retrooper.packetevents.packettype.PacketType;

public class PostCheck extends PacketCheck {

    private final byte packet;
    public long lastFlying, lastPacket;
    private boolean sent = false;

    public PostCheck(final GrimPlayer playerData, final byte packet) {
        super(playerData);

        this.packet = packet;
    }

    // Flag only when its both a post and a flag
    public void onPacketReceive(final PacketPlayReceiveEvent event) {
        if (PacketType.Play.Client.Util.isInstanceOfFlying(event.getPacketId())) {
            final long now = System.currentTimeMillis();
            final long delay = now - lastPacket;

            if (sent) {
                if (delay > 40L && delay < 100L) {
                    increaseBuffer(0.25);

                    if (getBuffer() > 0.5) {
                        debug("Failed check!");
                    }
                } else {
                    decreaseBuffer(0.025);
                }

                sent = false;
            }

            this.lastFlying = now;
        } else if (event.getPacketId() == packet) {
            final long now = System.currentTimeMillis();
            final long delay = now - lastFlying;

            if (delay < 10L) {
                lastPacket = now;
                sent = true;
            } else {
                decreaseBuffer(0.025);
            }
        }
    }
}
