package ac.grim.grimac.utils.data.packetentity;

import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;

// We use simple interpolation here to be "close enough"
public class PacketEntityTrackXRot extends PacketEntity {
    public float packetYaw;
    public float interpYaw;
    public int steps = 0;

    public PacketEntityTrackXRot(final GrimPlayer player, final EntityType type,
                                 final double x, final double y, final double z, final float xRot) {
        super(player, type, x, y, z);
        this.packetYaw = xRot;
        this.interpYaw = xRot;
    }

    @Override
    public void onMovement(final boolean highBound) {
        super.onMovement(highBound);
        if (steps > 0) {
            interpYaw = interpYaw + ((packetYaw - interpYaw) / steps--);
        }
    }
}
