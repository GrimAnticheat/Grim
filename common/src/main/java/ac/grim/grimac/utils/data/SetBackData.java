package ac.grim.grimac.utils.data;

import ac.grim.grimac.utils.math.Vector3dm;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class SetBackData {
    private final TeleportData teleportData;
    private final float xRot, yRot;
    private final double velocityX, velocityY, velocityZ;
    private boolean hasVelocity;
    private final boolean vehicle;
    private boolean isComplete = false;
    // TODO: Rethink when we block movements for teleports, perhaps after 10 ticks or 5 blocks?
    private boolean isPlugin;
    private int ticksComplete = 0;

    public SetBackData(TeleportData teleportData, float xRot, float yRot, double velocityX, double velocityY, double velocityZ, boolean hasVelocity, boolean vehicle, boolean isPlugin) {
        this.teleportData = teleportData;
        this.xRot = xRot;
        this.yRot = yRot;
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.velocityZ = velocityZ;
        this.hasVelocity = hasVelocity;
        this.vehicle = vehicle;
        this.isPlugin = isPlugin;
    }

    public SetBackData(TeleportData teleportData, float xRot, float yRot, double velocityX, double velocityY, double velocityZ, boolean vehicle, boolean isPlugin) {
        this(teleportData, xRot, yRot, velocityX, velocityY, velocityZ, true, vehicle, isPlugin);
    }

    public SetBackData(TeleportData teleportData, float xRot, float yRot, boolean vehicle, boolean isPlugin) {
        this(teleportData, xRot, yRot, 0, 0, 0, false, vehicle, isPlugin);
    }

    public void tick() {
        if (isComplete) ticksComplete++;
    }
}
