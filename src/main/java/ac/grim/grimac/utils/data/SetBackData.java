package ac.grim.grimac.utils.data;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.bukkit.Location;
import org.bukkit.util.Vector;

@Getter
@Setter
@ToString
public class SetBackData {
    Location position;
    float xRot, yRot;
    Vector velocity;
    Integer vehicle;
    boolean isComplete = false;
    boolean isPlugin;

    public SetBackData(Location position, float xRot, float yRot, Vector velocity, Integer vehicle, boolean isPlugin) {
        this.position = position;
        this.xRot = xRot;
        this.yRot = yRot;
        this.velocity = velocity;
        this.vehicle = vehicle;
        this.isPlugin = isPlugin;
    }
}
