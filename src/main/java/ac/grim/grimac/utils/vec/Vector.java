package ac.grim.grimac.utils.vec;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
@Accessors(chain = true)
public class Vector {
    private double x;
    private double y;
    private double z;

    public Vector() {
        this(0, 0, 0);
    }

    public Vector(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector clone() {
        return new Vector(this.x, this.y, this.z);
    }

    public Vector add(Vector other) {
        return this.add(other.x, other.y, other.z);
    }

    public Vector subtract(Vector other) {
        return this.subtract(other.x, other.y, other.z);
    }

    public Vector multiply(double multiplier) {
        this.x *= multiplier;
        this.y *= multiplier;
        this.z *= multiplier;
        return this;
    }

    public Vector multiply(Vector multiplier) {
        this.x *= multiplier.x;
        this.y *= multiplier.y;
        this.z *= multiplier.z;
        return this;
    }

    public Vector add(double x, double y, double z) {
        this.x += x;
        this.y += y;
        this.z += z;
        return this;
    }

    public Vector subtract(double x, double y, double z) {
        this.x -= x;
        this.y -= y;
        this.z -= z;
        return this;
    }

    public double distance(Vector other) {
        return Math.sqrt(this.distanceSquared(other));
    }

    public double length() {
        return Math.sqrt(this.lengthSquared());
    }

    public double lengthSquared() {
        return this.x * this.x + this.y * this.y + this.z * this.z;
    }

    public Vector normalize() {
        final double length = this.length();
        this.x /= length;
        this.y /= length;
        this.z /= length;
        return this;
    }

    public double distanceSquared(Vector other) {
        final double distX = (this.x - other.x) * (this.x - other.x);
        final double distY = (this.y - other.y) * (this.y - other.y);
        final double distZ = (this.z - other.z) * (this.z - other.z);
        return distX + distY + distZ;
    }

    public Vector crossProduct(@NotNull Vector o) {
        double newX = this.y * o.z - o.y * this.z;
        double newY = this.z * o.x - o.z * this.x;
        double newZ = this.x * o.y - o.x * this.y;
        this.x = newX;
        this.y = newY;
        this.z = newZ;
        return this;
    }

    public double dot(@NotNull Vector other) {
        return this.x * other.x + this.y * other.y + this.z * other.z;
    }
}
