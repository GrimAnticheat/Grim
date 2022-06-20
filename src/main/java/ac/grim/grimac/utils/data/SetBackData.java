package ac.grim.grimac.utils.data;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.bukkit.util.Vector;

@Getter
@Setter
@ToString
public class SetBackData {
    TeleportData teleportData;
    float xRot, yRot;
    Vector velocity;
    boolean vehicle;
    boolean isComplete = false;
    boolean isPlugin = false;

    public SetBackData(TeleportData teleportData, float xRot, float yRot, Vector velocity, boolean vehicle, boolean isPlugin) {
        this.teleportData = teleportData;
        this.xRot = xRot;
        this.yRot = yRot;
        this.velocity = velocity;
        this.vehicle = vehicle;
        this.isPlugin = isPlugin;
    }
}
