package org.abyssmc.reaperac.utils.nms;

import net.minecraft.server.v1_16_R3.Vec3D;
import net.minecraft.server.v1_16_R3.Vector3fa;

// <removed rant about spigot mappings>
// If I add 1.12 support, I can just extend this class with the other mappings...

public class Vec extends Vec3D {
    public Vec(double var0, double var2, double var4) {
        super(var0, var2, var4);
    }

    public Vec(Vector3fa var0) {
        super(var0);
    }

    /*protected void setX(int n) {
        this.x = n;
    }

    protected void setY(int n) {
        this.y = n;
    }

    protected void setZ(int n) {
        this.z = n;
    }

    public Vec3i above() {
        return this.above(1);
    }

    public Vec3i above(int n) {
        return this.relative(Direction.UP, n);
    }

    public Vec3i below() {
        return this.below(1);
    }

    public Vec3i below(int n) {
        return this.relative(Direction.DOWN, n);
    }

    public Vec3i relative(Direction direction, int n) {
        if (n == 0) {
            return this;
        }
        return new Vec3i(this.getX() + direction.getStepX() * n, this.getY() + direction.getStepY() * n, this.getZ() + direction.getStepZ() * n);
    }

    public Vec3i cross(Vec3i vec3i) {
        return new Vec3i(this.getY() * vec3i.getZ() - this.getZ() * vec3i.getY(), this.getZ() * vec3i.getX() - this.getX() * vec3i.getZ(), this.getX() * vec3i.getY() - this.getY() * vec3i.getX());
    }

    public boolean closerThan(Vec3i vec3i, double d) {
        return this.distSqr(vec3i.getX(), vec3i.getY(), vec3i.getZ(), false) < d * d;
    }

    public boolean closerThan(Position position, double d) {
        return this.distSqr(position.x(), position.y(), position.z(), true) < d * d;
    }

    public double distSqr(Vec3i vec3i) {
        return this.distSqr(vec3i.getX(), vec3i.getY(), vec3i.getZ(), true);
    }

    public double distSqr(Position position, boolean bl) {
        return this.distSqr(position.x(), position.y(), position.z(), bl);
    }

    public double distSqr(double d, double d2, double d3, boolean bl) {
        double d4 = bl ? 0.5 : 0.0;
        double d5 = (double)this.getX() + d4 - d;
        double d6 = (double)this.getY() + d4 - d2;
        double d7 = (double)this.getZ() + d4 - d3;
        return d5 * d5 + d6 * d6 + d7 * d7;
    }

    public int distManhattan(Vec3i vec3i) {
        float f = Math.abs(vec3i.getX() - this.getX());
        float f2 = Math.abs(vec3i.getY() - this.getY());
        float f3 = Math.abs(vec3i.getZ() - this.getZ());
        return (int)(f + f2 + f3);
    }

    public int get(Direction.Axis axis) {
        return axis.choose(this.x, this.y, this.z);
    }

    public String toString() {
        return MoreObjects.toStringHelper((Object)this).add("x", this.getX()).add("y", this.getY()).add("z", this.getZ()).toString();
    }

    public String toShortString() {
        return "" + this.getX() + ", " + this.getY() + ", " + this.getZ();
    }

    @Override
    public int compareTo(Object object) {
        return this.compareTo((Vec3i)object);
    }*/
}
