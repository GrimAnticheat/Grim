package ac.grim.grimac.utils.math;

import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3f;
import lombok.Getter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.Random;

public class Vector3dm implements Cloneable, Serializable {
    @Serial
    private static final long serialVersionUID = -2657651106777219169L;
    private static final Random random = new Random();
    public static final double epsilon = 1.0E-6;
    @Getter
    protected double x;
    @Getter
    protected double y;
    @Getter
    protected double z;

    @Contract(pure = true)
    public Vector3dm() {
        this.x = 0.0F;
        this.y = 0.0F;
        this.z = 0.0F;
    }

    @Contract(pure = true)
    public Vector3dm(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Contract(pure = true)
    public Vector3dm(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Contract(pure = true)
    public Vector3dm(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Contract("_, _ -> new")
    public static @NotNull Vector3dm min(@NotNull Vector3dm a, @NotNull Vector3dm b) {
        return new Vector3dm(Math.min(a.x, b.x), Math.min(a.y, b.y), Math.min(a.z, b.z));
    }

    @Contract("_, _ -> new")
    public static @NotNull Vector3dm max(@NotNull Vector3dm a, @NotNull Vector3dm b) {
        return new Vector3dm(Math.max(a.x, b.x), Math.max(a.y, b.y), Math.max(a.z, b.z));
    }

    @Contract(" -> new")
    public static @NotNull Vector3dm getRandom() {
        return new Vector3dm(random.nextDouble(), random.nextDouble(), random.nextDouble());
    }

    public @NotNull Vector3dm add(@NotNull Vector3dm vec) {
        return add(vec.x, vec.y, vec.z);
    }

    public @NotNull Vector3dm add(double x, double y, double z) {
        this.x += x;
        this.y += y;
        this.z += z;
        return this;
    }

    public @NotNull Vector3dm subtract(@NotNull Vector3dm vec) {
        return subtract(vec.x, vec.y, vec.z);
    }

    public @NotNull Vector3dm subtract(double x, double y, double z) {
        this.x -= x;
        this.y -= y;
        this.z -= z;
        return this;
    }

    public @NotNull Vector3dm multiply(@NotNull Vector3dm vec) {
        return multiply(vec.x, vec.y, vec.z);
    }

    public @NotNull Vector3dm multiply(double x, double y, double z) {
        this.x *= x;
        this.y *= y;
        this.z *= z;
        return this;
    }

    public @NotNull Vector3dm divide(@NotNull Vector3dm vec) {
        this.x /= vec.x;
        this.y /= vec.y;
        this.z /= vec.z;
        return this;
    }

    public @NotNull Vector3dm copy(@NotNull Vector3dm vec) {
        this.x = vec.x;
        this.y = vec.y;
        this.z = vec.z;
        return this;
    }

    public double length() {
        return Math.sqrt(GrimMath.square(this.x) + GrimMath.square(this.y) + GrimMath.square(this.z));
    }

    public double lengthSquared() {
        return GrimMath.square(this.x) + GrimMath.square(this.y) + GrimMath.square(this.z);
    }

    public double distance(@NotNull Vector3dm o) {
        return Math.sqrt(distanceSquared(o));
    }

    public double distanceSquared(@NotNull Vector3dm o) {
        return distanceSquared(o.x, o.y, o.z);
    }

    public double distance(double oX, double oY, double oZ) {
        return Math.sqrt(distanceSquared(oX, oY, oZ));
    }

    public double distanceSquared(double oX, double oY, double oZ) {
        return GrimMath.square(this.x - oX) + GrimMath.square(this.y - oY) + GrimMath.square(this.z - oZ);
    }

    public @NotNull Vector3dm midpoint(@NotNull Vector3dm other) {
        this.x = (this.x + other.x) / (double) 2.0F;
        this.y = (this.y + other.y) / (double) 2.0F;
        this.z = (this.z + other.z) / (double) 2.0F;
        return this;
    }

    public @NotNull Vector3dm getMidpoint(@NotNull Vector3dm other) {
        double x = (this.x + other.x) / (double) 2.0F;
        double y = (this.y + other.y) / (double) 2.0F;
        double z = (this.z + other.z) / (double) 2.0F;
        return new Vector3dm(x, y, z);
    }

    public @NotNull Vector3dm multiply(int m) {
        this.x *= m;
        this.y *= m;
        this.z *= m;
        return this;
    }

    public @NotNull Vector3dm multiply(double m) {
        this.x *= m;
        this.y *= m;
        this.z *= m;
        return this;
    }

    public @NotNull Vector3dm multiply(float m) {
        this.x *= m;
        this.y *= m;
        this.z *= m;
        return this;
    }

    public double dot(@NotNull Vector3dm other) {
        return this.x * other.x + this.y * other.y + this.z * other.z;
    }

    public @NotNull Vector3dm crossProduct(@NotNull Vector3dm o) {
        double newX = this.y * o.z - o.y * this.z;
        double newY = this.z * o.x - o.z * this.x;
        double newZ = this.x * o.y - o.x * this.y;
        this.x = newX;
        this.y = newY;
        this.z = newZ;
        return this;
    }

    public @NotNull Vector3dm getCrossProduct(@NotNull Vector3dm o) {
        double x = this.y * o.z - o.y * this.z;
        double y = this.z * o.x - o.z * this.x;
        double z = this.x * o.y - o.x * this.y;
        return new Vector3dm(x, y, z);
    }

    public @NotNull Vector3dm normalize() {
        double length = this.length();
        this.x /= length;
        this.y /= length;
        this.z /= length;
        return this;
    }

    public @NotNull Vector3dm zero() {
        this.x = 0.0F;
        this.y = 0.0F;
        this.z = 0.0F;
        return this;
    }

    public boolean isZero() {
        return this.x == (double) 0.0F && this.y == (double) 0.0F && this.z == (double) 0.0F;
    }

    @NotNull Vector3dm normalizeZeros() {
        if (this.x == (double) -0.0F) {
            this.x = 0.0F;
        }

        if (this.y == (double) -0.0F) {
            this.y = 0.0F;
        }

        if (this.z == (double) -0.0F) {
            this.z = 0.0F;
        }

        return this;
    }

    public boolean isInAABB(@NotNull Vector3dm min, @NotNull Vector3dm max) {
        return this.x >= min.x && this.x <= max.x && this.y >= min.y && this.y <= max.y && this.z >= min.z && this.z <= max.z;
    }

    public boolean isInSphere(@NotNull Vector3dm origin, double radius) {
        return GrimMath.square(origin.x - this.x) + GrimMath.square(origin.y - this.y) + GrimMath.square(origin.z - this.z) <= GrimMath.square(radius);
    }

    public boolean isNormalized() {
        return Math.abs(this.lengthSquared() - (double) 1.0F) < epsilon;
    }

    public @NotNull Vector3dm rotateAroundX(double angle) {
        double angleCos = Math.cos(angle);
        double angleSin = Math.sin(angle);
        double y = angleCos * this.getY() - angleSin * this.getZ();
        double z = angleSin * this.getY() + angleCos * this.getZ();
        return this.setY(y).setZ(z);
    }

    public @NotNull Vector3dm rotateAroundY(double angle) {
        double angleCos = Math.cos(angle);
        double angleSin = Math.sin(angle);
        double x = angleCos * this.getX() + angleSin * this.getZ();
        double z = -angleSin * this.getX() + angleCos * this.getZ();
        return this.setX(x).setZ(z);
    }

    public @NotNull Vector3dm rotateAroundZ(double angle) {
        double angleCos = Math.cos(angle);
        double angleSin = Math.sin(angle);
        double x = angleCos * this.getX() - angleSin * this.getY();
        double y = angleSin * this.getX() + angleCos * this.getY();
        return this.setX(x).setY(y);
    }

    public @NotNull Vector3dm setX(int x) {
        this.x = x;
        return this;
    }

    public @NotNull Vector3dm setX(double x) {
        this.x = x;
        return this;
    }

    public @NotNull Vector3dm setX(float x) {
        this.x = x;
        return this;
    }

    public int getBlockX() {
        return GrimMath.mojangFloor(this.x);
    }

    public @NotNull Vector3dm setY(int y) {
        this.y = y;
        return this;
    }

    public @NotNull Vector3dm setY(double y) {
        this.y = y;
        return this;
    }

    public @NotNull Vector3dm setY(float y) {
        this.y = y;
        return this;
    }

    public int getBlockY() {
        return GrimMath.mojangFloor(this.y);
    }

    public @NotNull Vector3dm setZ(int z) {
        this.z = z;
        return this;
    }

    public @NotNull Vector3dm setZ(double z) {
        this.z = z;
        return this;
    }

    public @NotNull Vector3dm setZ(float z) {
        this.z = z;
        return this;
    }

    public int getBlockZ() {
        return GrimMath.mojangFloor(this.z);
    }

    @Contract(value = "null -> false", pure = true)
    public boolean equals(Object obj) {
        return obj instanceof Vector3dm other && Math.abs(this.x - other.x) < 1.0E-6 && Math.abs(this.y - other.y) < 1.0E-6 && Math.abs(this.z - other.z) < 1.0E-6 && this.getClass().equals(obj.getClass());
    }

    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + Long.hashCode(Double.doubleToLongBits(this.x));
        hash = 79 * hash + Long.hashCode(Double.doubleToLongBits(this.y));
        hash = 79 * hash + Long.hashCode(Double.doubleToLongBits(this.z));
        return hash;
    }

    public @NotNull Vector3dm clone() {
        try {
            return (Vector3dm) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new Error(e);
        }
    }

    public String toString() {
        return this.x + "," + this.y + "," + this.z;
    }

    public @NotNull Vector3f toVector3f() {
        return new Vector3f((float) this.x, (float) this.y, (float) this.z);
    }

    public @NotNull Vector3d toVector3d() {
        return new Vector3d(this.x, this.y, this.z);
    }
}
