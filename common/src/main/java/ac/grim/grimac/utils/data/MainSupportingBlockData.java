package ac.grim.grimac.utils.data;

import com.github.retrooper.packetevents.util.Vector3i;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

@Data
@AllArgsConstructor
public class MainSupportingBlockData {
    @Nullable
    Vector3i blockPos;
    boolean onGround;

    public boolean lastOnGroundAndNoBlock() {
        return blockPos == null && onGround;
    }
}
