package ac.grim.grimac.utils.nmsImplementations;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import org.bukkit.Particle;
import org.bukkit.util.Vector;

import java.util.ArrayList;

// Class copied from https://www.spigotmc.org/threads/hitboxes-and-ray-tracing.174358/
public class RayTrace {

    //origin = start position
    //direction = direction in which the raytrace will go
    Vector origin, direction;

    public RayTrace(Vector origin, Vector direction) {
        this.origin = origin;
        this.direction = direction;
    }

    public RayTrace(GrimPlayer player, double x, double y, double z, float xRot, float yRot) {
        this.origin = new Vector(x, y, z);
        this.direction = getDirection(player, xRot, yRot);
    }

    // Account for ShitMath by using player's trig handler
    // Copied from hawk which probably copied it from NMS
    public static Vector getDirection(GrimPlayer player, float xRot, float yRot) {
        Vector vector = new Vector();
        float rotX = (float) Math.toRadians(xRot);
        float rotY = (float) Math.toRadians(yRot);
        vector.setY(-player.trigHandler.sin(rotY));
        double xz = player.trigHandler.cos(rotY);
        vector.setX(-xz * player.trigHandler.sin(rotX));
        vector.setZ(xz * player.trigHandler.cos(rotX));
        return vector;
    }

    //checks if a position is on contained within the position
    public boolean isOnLine(Vector position) {
        double t = (position.getX() - origin.getX()) / direction.getX();
        return position.getBlockY() == origin.getY() + (t * direction.getY()) && position.getBlockZ() == origin.getZ() + (t * direction.getZ());
    }

    //intersection detection for current raytrace with return
    public Vector positionOfIntersection(Vector min, Vector max, double blocksAway, double accuracy) {
        ArrayList<Vector> positions = traverse(blocksAway, accuracy);
        for (Vector position : positions) {
            if (intersects(position, min, max)) {
                return position;
            }
        }
        return null;
    }

    //get all postions on a raytrace
    public ArrayList<Vector> traverse(double blocksAway, double accuracy) {
        ArrayList<Vector> positions = new ArrayList<>();
        for (double d = 0; d <= blocksAway; d += accuracy) {
            positions.add(getPostion(d));
        }
        return positions;
    }

    //general intersection detection
    public static boolean intersects(Vector position, Vector min, Vector max) {
        if (position.getX() < min.getX() || position.getX() > max.getX()) {
            return false;
        } else if (position.getY() < min.getY() || position.getY() > max.getY()) {
            return false;
        } else return !(position.getZ() < min.getZ()) && !(position.getZ() > max.getZ());
    }

    //get a point on the raytrace at X blocks away
    public Vector getPostion(double blocksAway) {
        return origin.clone().add(direction.clone().multiply(blocksAway));
    }

    //intersection detection for current raytrace
    public boolean intersects(Vector min, Vector max, double blocksAway, double accuracy) {
        ArrayList<Vector> positions = traverse(blocksAway, accuracy);
        for (Vector position : positions) {
            if (intersects(position, min, max)) {
                return true;
            }
        }
        return false;
    }

    //bounding box instead of vector
    public Vector positionOfIntersection(SimpleCollisionBox boundingBox, double blocksAway, double accuracy) {
        ArrayList<Vector> positions = traverse(blocksAway, accuracy);
        for (Vector position : positions) {
            if (intersects(position, boundingBox.min(), boundingBox.max())) {
                return position;
            }
        }
        return null;
    }

    //bounding box instead of vector
    public boolean intersects(SimpleCollisionBox boundingBox, double blocksAway, double accuracy) {
        ArrayList<Vector> positions = traverse(blocksAway, accuracy);
        for (Vector position : positions) {
            if (intersects(position, boundingBox.min(), boundingBox.max())) {
                return true;
            }
        }
        return false;
    }

    //debug / effects
    public void highlight(GrimPlayer player, double blocksAway, double accuracy) {
        for (Vector position : traverse(blocksAway, accuracy)) {
            player.bukkitPlayer.spawnParticle(Particle.NOTE, position.getX(), position.getY(), position.getZ(), 1);
        }
    }

    // Returns first player in the specified player's line of sight
    // up to max blocks away, or null if none.
    /*public static PacketEntity getTargetEntity(GrimPlayer player, int max) {
        Ray ray = Ray.from(player);
        double d = -1;
        PacketEntity closest = null;
        for (PacketEntity player1 : player.compensatedEntities.entityMap.values()) {
            double dis = GetBoundingBox.getPacketEntityBoundingBox(player1.position.x, player1.position.y, player1.position.z, player1);(ray, 0, max);
            if (dis != -1) {
                if (dis < d || d == -1) {
                    d = dis;
                    closest = player1;
                }
            }
        }
        return closest;
    }*/
}