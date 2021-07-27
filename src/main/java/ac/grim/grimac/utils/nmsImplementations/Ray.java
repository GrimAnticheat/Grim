package ac.grim.grimac.utils.nmsImplementations;

import ac.grim.grimac.player.GrimPlayer;
import fr.mrmicky.fastparticles.ParticleType;
import io.github.retrooper.packetevents.utils.pair.Pair;
import org.bukkit.util.Vector;

// Copied directly from Hawk
public class Ray implements Cloneable {

    private Vector origin;
    private Vector direction;

    public Ray(Vector origin, Vector direction) {
        this.origin = origin;
        this.direction = direction;
    }

    public Ray clone() {
        Ray clone;
        try {
            clone = (Ray) super.clone();
            clone.origin = this.origin.clone();
            clone.direction = this.direction.clone();
            return clone;
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String toString() {
        return "origin: " + origin + " direction: " + direction;
    }

    public void highlight(GrimPlayer player, double blocksAway, double accuracy) {
        for (double x = 0; x < blocksAway; x += accuracy) {
            Vector point = getPointAtDistance(x);
            ParticleType flame = ParticleType.of("REDSTONE");
            flame.spawn(player.bukkitPlayer, point.getX(), point.getY(), point.getZ(), 1);
        }
    }

    public Vector getPointAtDistance(double distance) {
        Vector dir = new Vector(direction.getX(), direction.getY(), direction.getZ());
        Vector orig = new Vector(origin.getX(), origin.getY(), origin.getZ());
        return orig.add(dir.multiply(distance));
    }

    //https://en.wikipedia.org/wiki/Skew_lines#Nearest_Points
    public Pair<Vector, Vector> closestPointsBetweenLines(Ray other) {
        Vector n1 = direction.clone().crossProduct(other.direction.clone().crossProduct(direction));
        Vector n2 = other.direction.clone().crossProduct(direction.clone().crossProduct(other.direction));

        Vector c1 = origin.clone().add(direction.clone().multiply(other.origin.clone().subtract(origin).dot(n2) / direction.dot(n2)));
        Vector c2 = other.origin.clone().add(other.direction.clone().multiply(origin.clone().subtract(other.origin).dot(n1) / other.direction.dot(n1)));

        return new Pair<>(c1, c2);
    }

    public Vector getOrigin() {
        return origin;
    }

    public Vector getDirection() {
        return direction;
    }
}
