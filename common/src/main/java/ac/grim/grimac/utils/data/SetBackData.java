package ac.grim.grimac.utils.data;

import ac.grim.grimac.utils.math.Vector3dm;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class SetBackData {
    TeleportData teleportData;
    float xRot, yRot;
    Vector3dm velocity;
    boolean vehicle;
    boolean isComplete = false;
    // TODO: Rethink when we block movements for teleports, perhaps after 10 ticks or 5 blocks?
    boolean isPlugin = false;
    int ticksComplete = 0;

    public SetBackData(TeleportData teleportData, float xRot, float yRot, Vector3dm velocity, boolean vehicle, boolean isPlugin) {
        this.teleportData = teleportData;
        this.xRot = xRot;
        this.yRot = yRot;
        this.velocity = velocity;
        this.vehicle = vehicle;
        this.isPlugin = isPlugin;
    }

    public void tick() {
        if (isComplete) ticksComplete++;
    }
}
