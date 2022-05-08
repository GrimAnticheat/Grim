package ac.grim.grimac.utils.data;

import com.github.retrooper.packetevents.util.Vector3d;
import org.bukkit.util.Vector;

public class SetbackLocationVelocity {
    public Vector3d position;
    Vector velocity;

    public SetbackLocationVelocity(Vector3d position) {
        this.position = position;
        this.velocity = null;
    }

    public SetbackLocationVelocity(Vector3d position, Vector velocity) {
        this.position = position;
        this.velocity = velocity;
    }
}
