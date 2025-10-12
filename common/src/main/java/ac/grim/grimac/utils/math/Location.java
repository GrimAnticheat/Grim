package ac.grim.grimac.utils.math;

import ac.grim.grimac.platform.api.world.PlatformWorld;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.Objects;

public class Location implements Cloneable {
    private @Nullable WeakReference<PlatformWorld> world;
    @Getter
    @Setter
    private double x;
    @Getter
    @Setter
    private double y;
    @Getter
    @Setter
    private double z;
    @Getter
    @Setter
    private float pitch;
    @Getter
    @Setter
    private float yaw;

    public Location(PlatformWorld world, double x, double y, double z) {
        this(world, x, y, z, 0.0F, 0.0F);
    }

    public Location(PlatformWorld world, double x, double y, double z, float yaw, float pitch) {
        if (world != null) {
            this.world = new WeakReference<>(world);
        }

        this.x = x;
        this.y = y;
        this.z = z;
        this.pitch = pitch;
        this.yaw = yaw;
    }

    public static float normalizeYaw(float yaw) {
        yaw %= 360.0F;
        if (yaw >= 180.0F) {
            yaw -= 360.0F;
        } else if (yaw < -180.0F) {
            yaw += 360.0F;
        }

        return yaw;
    }

    public static float normalizePitch(float pitch) {
        if (pitch > 90.0F) {
            pitch = 90.0F;
        } else if (pitch < -90.0F) {
            pitch = -90.0F;
        }

        return pitch;
    }

    public PlatformWorld getWorld() {
        if (this.world == null) {
            return null;
        } else {
            return this.world.get();
        }
    }

    public void setWorld(@Nullable PlatformWorld world) {
        this.world = world == null ? null : new WeakReference<>(world);
    }

    public @NotNull Location add(@NotNull Location vec) {
        if (Objects.requireNonNull(vec).getWorld() == this.getWorld()) {
            this.x += vec.x;
            this.y += vec.y;
            this.z += vec.z;
            return this;
        } else {
            throw new IllegalArgumentException("Cannot add Locations of differing worlds");
        }
    }

    public @NotNull Location add(double x, double y, double z) {
        this.x += x;
        this.y += y;
        this.z += z;
        return this;
    }

    public @NotNull Location subtract(@NotNull Location vec) {
        if (Objects.requireNonNull(vec).getWorld() == this.getWorld()) {
            this.x -= vec.x;
            this.y -= vec.y;
            this.z -= vec.z;
            return this;
        } else {
            throw new IllegalArgumentException("Cannot add Locations of differing worlds");
        }
    }

    public @NotNull Location subtract(double x, double y, double z) {
        this.x -= x;
        this.y -= y;
        this.z -= z;
        return this;
    }

    public double distance(@NotNull Location o) {
        return Math.sqrt(this.distanceSquared(o));
    }

    public double distanceSquared(@NotNull Location o) {
        if (o.getWorld() != null && this.getWorld() != null) {
            if (o.getWorld() != this.getWorld()) {
                throw new IllegalArgumentException("Cannot measure distance between " + this.getWorld().getName() + " and " + o.getWorld().getName());
            } else {
                return (this.x - o.x) * (this.x - o.x) + (this.y - o.y) * (this.y - o.y) + (this.z - o.z) * (this.z - o.z);
            }
        } else {
            throw new IllegalArgumentException("Cannot measure distance to a null world");
        }
    }

    public @NotNull Location multiply(double m) {
        this.x *= m;
        this.y *= m;
        this.z *= m;
        return this;
    }

    public @NotNull Location zero() {
        this.x = 0.0F;
        this.y = 0.0F;
        this.z = 0.0F;
        return this;
    }

    public @NotNull Location set(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    public @NotNull Location add(@NotNull Location base, double x, double y, double z) {
        return this.set(base.x + x, base.y + y, base.z + z);
    }

    public @NotNull Location subtract(@NotNull Location base, double x, double y, double z) {
        return this.set(base.x - x, base.y - y, base.z - z);
    }

    public boolean equals(Object obj) {
        if (obj == null || this.getClass() != obj.getClass()) {
            return false;
        } else {
            Location other = (Location) obj;
            return Objects.equals(this.world == null ? null : this.world.get(), other.world == null ? null : other.world.get())
                    && Double.doubleToLongBits(this.x) == Double.doubleToLongBits(other.x)
                    && Double.doubleToLongBits(this.y) == Double.doubleToLongBits(other.y)
                    && Double.doubleToLongBits(this.z) == Double.doubleToLongBits(other.z)
                    && Float.floatToIntBits(this.pitch) == Float.floatToIntBits(other.pitch)
                    && Float.floatToIntBits(this.yaw) == Float.floatToIntBits(other.yaw);
        }
    }

    public int hashCode() {
        int hash = 3;
        PlatformWorld world = this.world == null ? null : this.world.get();
        hash = 19 * hash + (world != null ? world.hashCode() : 0);
        hash = 19 * hash + Long.hashCode(Double.doubleToLongBits(this.x));
        hash = 19 * hash + Long.hashCode(Double.doubleToLongBits(this.y));
        hash = 19 * hash + Long.hashCode(Double.doubleToLongBits(this.z));
        hash = 19 * hash + Float.floatToIntBits(this.pitch);
        hash = 19 * hash + Float.floatToIntBits(this.yaw);
        return hash;
    }

    public String toString() {
        return "Location{world=" + (this.world == null ? null : this.world.get()) + ",x=" + this.x + ",y=" + this.y + ",z=" + this.z + ",pitch=" + this.pitch + ",yaw=" + this.yaw + "}";
    }

    public @NotNull Location clone() {
        try {
            return (Location) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new Error(e);
        }
    }

    public double x() {
        return this.getX();
    }

    public int getBlockX() {
        return GrimMath.mojangFloor(x);
    }

    public double y() {
        return this.getY();
    }

    public int getBlockY() {
        return GrimMath.mojangFloor(y);
    }

    public double z() {
        return this.getZ();
    }

    public int getBlockZ() {
        return GrimMath.mojangFloor(z);
    }

    public boolean isWorldLoaded() {
        if (this.world == null) {
            return false;
        } else {
            PlatformWorld world = this.world.get();
            return world != null && world.isLoaded();
        }
    }

    public Vector3dm toVector() {
        return new Vector3dm(x, y, z);
    }
}
