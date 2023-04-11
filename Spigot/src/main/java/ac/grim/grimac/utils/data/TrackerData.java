package ac.grim.grimac.utils.data;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class TrackerData {
    @NonNull
    double x, y, z;
    @NonNull
    float xRot, yRot;
    @NonNull
    EntityType entityType;
    @NonNull
    int lastTransactionHung;
    int legacyPointEightMountedUpon;
}
