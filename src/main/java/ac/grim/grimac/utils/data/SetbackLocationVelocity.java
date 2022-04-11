package ac.grim.grimac.utils.data;

import com.github.retrooper.packetevents.util.Vector3d;
import org.bukkit.Location;
import org.bukkit.util.Vector;

public class SetbackLocationVelocity {
    public Location position;
    Vector velocity;

    public SetbackLocationVelocity(Vector3d vector3d) {
        this.position = new Location(null, vector3d.getX(), vector3d.getY(), vector3d.getZ());
        this.velocity = null;
    }

    public SetbackLocationVelocity(Vector3d vector3d, Vector velocity) {
        this.position = new Location(null, vector3d.getX(), vector3d.getY(), vector3d.getZ());
        this.velocity = velocity;
    }
}
