package ac.grim.grimac.utils;

import ac.grim.grimac.utils.math.GrimMath;

public record Vec2(float x, float y) {

    public static final Vec2 ZERO = new Vec2(0.0F, 0.0F);

    public Vec2 scale(float scalar) {
        return new Vec2(this.x * scalar, this.y * scalar);
    }

    public float dot(Vec2 vec) {
        return this.x * vec.x + this.y * vec.y;
    }

    public Vec2 add(Vec2 vec) {
        return new Vec2(this.x + vec.x, this.y + vec.y);
    }

    public Vec2 add(float vec) {
        return new Vec2(this.x + vec, this.y + vec);
    }

    public boolean equals(Vec2 vec) {
        return this.x == vec.x && this.y == vec.y;
    }

    public Vec2 normalized() {
        float length = GrimMath.sqrt(this.x * this.x + this.y * this.y);
        return length < 1.0E-4F ? ZERO : new Vec2(this.x / length, this.y / length);
    }

    public float length() {
        return GrimMath.sqrt(this.x * this.x + this.y * this.y);
    }

    public float lengthSquared() {
        return this.x * this.x + this.y * this.y;
    }

    public float distanceToSqr(Vec2 vec) {
        float f = vec.x - this.x;
        float f1 = vec.y - this.y;
        return f * f + f1 * f1;
    }

    public Vec2 negated() {
        return new Vec2(-this.x, -this.y);
    }
}
