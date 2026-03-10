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
public final class BlockPrediction {
    private List<Vector3i> forBlockUpdate;
    private Vector3i blockPosition;
    private int originalBlockId;
    private final Vector3d playerPosition;
}
