package ac.grim.grimac.utils.data;

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
    int lastTransactionHung;
}
