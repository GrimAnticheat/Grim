package ac.grim.grimac.utils.data;

import ac.grim.grimac.utils.nmsutil.WatchableIndexUtil;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
public class TrackerData {
    EntityType type;

    double x;
    double y;
    double z;
    float xRot;
    float yRot;

    Integer data;
    Integer hookedEntity;

    int lastTransactionHung;

    public TrackerData(EntityType type, double x, double y, double z, float xRot, float yRot, Integer data, int lastTransactionHung) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.z = z;
        this.xRot = xRot;
        this.yRot = yRot;
        this.data = data;
        this.lastTransactionHung = lastTransactionHung;
    }

    public void updateMetadata(List<EntityData> data) {
        if (type == EntityTypes.FISHING_BOBBER) {
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
