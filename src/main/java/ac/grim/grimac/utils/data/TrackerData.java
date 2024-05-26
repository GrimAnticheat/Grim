package ac.grim.grimac.utils.data;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class TrackerData {
    private @NonNull double x, y, z;
    private @NonNull float xRot, yRot;
    private @NonNull EntityType entityType;
    private @NonNull int lastTransactionHung;
    private int legacyPointEightMountedUpon;
}
