package ac.grim.grimac.utils.anticheat.update;

import ac.grim.grimac.utils.data.SetBackData;
import com.github.retrooper.packetevents.util.Vector3d;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public final class PositionUpdate {
    private final Vector3d from, to;
    private final boolean onGround;
    private boolean isTeleport;
    private final SetBackData setback;
}