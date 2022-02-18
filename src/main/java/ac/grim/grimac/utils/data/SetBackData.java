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
    int trans;
    boolean isComplete = false;
    boolean isPlugin = false;

    public SetBackData(Location position, float xRot, float yRot, Vector velocity, Integer vehicle, int trans, boolean isPlugin) {
        this.position = position;
        this.xRot = xRot;
        this.yRot = yRot;
        this.velocity = velocity;
        this.vehicle = vehicle;
        this.trans = trans;
        this.isPlugin = isPlugin;
    }
}
