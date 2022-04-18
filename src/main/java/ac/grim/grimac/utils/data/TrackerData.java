package ac.grim.grimac.utils.data;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TrackerData {
    double x;
    double y;
    double z;
    float xRot;
    float yRot;
    EntityType entityType;
    int lastTransactionHung;
}
