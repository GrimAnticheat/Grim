package ac.grim.grimac.utils.data;

import ac.grim.grimac.utils.nmsutil.WatchableIndexUtil;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Data
public class TrackerData {
    double x, y, z;
    float xRot, yRot;
    @NotNull
    EntityType entityType;
    int lastTransactionHung;
    int legacyPointEightMountedUpon;
    Integer data;
    Integer hookedEntity;

    public TrackerData(double x, double y, double z, float xRot, float yRot, @NotNull EntityType entityType, int lastTransactionHung, Integer data) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.xRot = xRot;
        this.yRot = yRot;
        this.entityType = entityType;
        this.lastTransactionHung = lastTransactionHung;
        this.data = data;
    }

    public void updateMetadata(List<EntityData> data) {
        if (entityType == EntityTypes.FISHING_BOBBER) {
            EntityData hookedEntityData = WatchableIndexUtil.getIndex(data, 8);
            if (hookedEntityData != null) {
                Integer value = (Integer) hookedEntityData.getValue();
                if (value == 0) {
                    this.hookedEntity = null;
                } else {
                    this.hookedEntity = value - 1;
                }
            }
        }
    }
}
