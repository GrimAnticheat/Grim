package ac.grim.grimac.utils.nmsutil;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.Pair;
import ac.grim.grimac.utils.math.Vector3dm;
import lombok.Getter;

// Copied directly from Hawk
@Getter
public class Ray implements Cloneable {

    private final Vector3dm origin;
    private final Vector3dm direction;

    public Ray(Vector3dm origin, Vector3dm direction) {
        this.origin = origin;
        this.direction = direction;
    }

    public Ray(GrimPlayer player, double x, double y, double z, float xRot, float yRot) {
        this.origin = new Vector3dm(x, y, z);
        this.direction = calculateDirection(player, xRot, yRot);
    }

    // Account for FastMath by using player's trig handler
    // Copied from hawk which probably copied it from NMS
    public static Vector3dm calculateDirection(GrimPlayer player, float xRot, float yRot) {
        Vector3dm vector = new Vector3dm();
        float rotX = (float) Math.toRadians(xRot);
        float rotY = (float) Math.toRadians(yRot);
        vector.setY(-player.trigHandler.sin(rotY));
        double xz = player.trigHandler.cos(rotY);
        vector.setX(-xz * player.trigHandler.sin(rotX));
        vector.setZ(xz * player.trigHandler.cos(rotX));
        return vector;
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public Ray clone() {
        return new Ray(this.origin.clone(), this.direction.clone());
    }

    public String toString() {
        return "origin: " + origin + " direction: " + direction;
    }

    public Vector3dm getPointAtDistance(double distance) {
        Vector3dm dir = new Vector3dm(direction.getX(), direction.getY(), direction.getZ());
        Vector3dm orig = new Vector3dm(origin.getX(), origin.getY(), origin.getZ());
        return orig.add(dir.multiply(distance));
    }

    // https://en.wikipedia.org/wiki/Skew_lines#Nearest_Points
    public Pair<Vector3dm, Vector3dm> closestPointsBetweenLines(Ray other) {
        Vector3dm n1 = direction.clone().crossProduct(other.direction.clone().crossProduct(direction));
        Vector3dm n2 = other.direction.clone().crossProduct(direction.clone().crossProduct(other.direction));

        Vector3dm c1 = origin.clone().add(direction.clone().multiply(other.origin.clone().subtract(origin).dot(n2) / direction.dot(n2)));
        Vector3dm c2 = other.origin.clone().add(other.direction.clone().multiply(origin.clone().subtract(other.origin).dot(n1) / other.direction.dot(n1)));

        return new Pair<>(c1, c2);
    }
}
