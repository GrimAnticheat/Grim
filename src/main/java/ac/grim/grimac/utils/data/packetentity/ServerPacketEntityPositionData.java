package ac.grim.grimac.utils.data.packetentity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ServerPacketEntityPositionData {
    double x;
    double y;
    double z;
    float xRot;
    float yRot;
}
