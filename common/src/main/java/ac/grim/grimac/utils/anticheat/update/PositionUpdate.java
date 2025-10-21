package ac.grim.grimac.utils.anticheat.update;

import ac.grim.grimac.utils.data.SetBackData;
import ac.grim.grimac.utils.data.TeleportData;
import com.github.retrooper.packetevents.util.Vector3d;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public final class PositionUpdate {
    private final double fromX;
    private final double fromY;
    private final double fromZ;
    private final double toX;
    private final double toY;
    private final double toZ;
    private final boolean onGround;
    private final SetBackData setback;
    private final TeleportData teleportData;
    private boolean isTeleport;
}
