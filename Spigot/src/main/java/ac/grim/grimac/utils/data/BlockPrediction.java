package ac.grim.grimac.utils.data;

import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3i;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@Getter
@Setter
public class BlockPrediction {
    List<Vector3i> forBlockUpdate;
    Vector3i blockPosition;
    int originalBlockId;
    Vector3d playerPosition;
}
