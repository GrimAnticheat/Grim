package ac.grim.grimac.utils.data;

import io.github.retrooper.packetevents.utils.vector.Vector3d;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.World;
import org.bukkit.util.Vector;

@Getter
@Setter
public class SetBackData {
    World world;
    Vector3d position;
    float xRot, yRot;
    Vector velocity;
    Integer vehicle;
    int trans;
    boolean isComplete = false;

    public SetBackData(World world, Vector3d position, float xRot, float yRot, Vector velocity, Integer vehicle, int trans) {
        this.world = world;
        this.position = position;
        this.xRot = xRot;
        this.yRot = yRot;
        this.velocity = velocity;
        this.vehicle = vehicle;
        this.trans = trans;
    }
}
