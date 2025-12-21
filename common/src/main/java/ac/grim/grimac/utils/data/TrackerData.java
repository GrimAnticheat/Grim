package ac.grim.grimac.utils.data;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import lombok.Data;

@Data
public class TrackerData {
    private double x, y, z;
    private float xRot, yRot;
    private EntityType entityType;
    private int lastTransactionHung;
    private int legacyPointEightMountedUpon;

    public TrackerData(double x, double y, double z, float xRot, float yRot, EntityType entityType, int lastTransactionHung) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.xRot = xRot;
        this.yRot = yRot;
        this.entityType = entityType;
        this.lastTransactionHung = lastTransactionHung;
    }
}
