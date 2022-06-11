package ac.grim.grimac.utils.data;

import com.github.retrooper.packetevents.util.Vector3i;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ClientBlockPrediction {
    int sequenceId;
    int blockId;
    Vector3i position;
}
