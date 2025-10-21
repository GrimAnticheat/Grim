package ac.grim.grimac.utils.nmsutil;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.Pair;
import ac.grim.grimac.utils.math.Vector3dm;
import lombok.Getter;

// Copied directly from Hawk
@Getter
public class Ray implements Cloneable {

    public final double originX;
    public final double originY;
    public final double originZ;

    public final double dirX;
    public final double dirY;
    public final double dirZ;

    public Ray(double originX, double originY, double originZ, double dirX, double dirY, double dirZ) {
        this.originX = originX;
        this.originY = originY;
        this.originZ = originZ;
        this.dirX = dirX;
        this.dirY = dirY;
        this.dirZ = dirZ;
    }

    public Ray(GrimPlayer player, double x, double y, double z, float xRot, float yRot) {
        this.originX = x;
        this.originY = y;
        this.originZ = z;

        // Account for FastMath by using player's trig handler
        // Copied from hawk which probably copied it from NMS
        float rotX = (float) Math.toRadians(xRot);
        float rotY = (float) Math.toRadians(yRot);
        dirY = -player.trigHandler.sin(rotY);
        double xz = player.trigHandler.cos(rotY);
        dirX = -xz * player.trigHandler.sin(rotX);
        dirZ = xz * player.trigHandler.cos(rotX);
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public Ray clone() {
        return new Ray(this.originX, this.originY, this.originZ, this.dirX, this.dirY, this.dirZ);
    }

    public String toString() {
        return "origin: " + this.originX + "," + this.originY + "," + this.originZ + " direction: " + this.dirX + "," + this.dirY + "," + this.dirZ;
    }

    public Vector3dm getPointAtDistance(double distance) {
        return new Vector3dm(
                this.originX + this.dirX * distance,
                this.originY + this.dirY * distance,
                this.originZ + this.dirZ * distance
        );
    }
}
