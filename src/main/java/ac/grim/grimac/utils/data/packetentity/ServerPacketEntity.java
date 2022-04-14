package ac.grim.grimac.utils.data.packetentity;

import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ServerPacketEntity {
    double x;
    double y;
    double z;
    float xRot;
    float yRot;

    Integer data;
    List<EntityData> metadata;
    int lastTransactionHung;

}
