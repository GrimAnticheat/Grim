package ac.grim.grimac.utils.data;

import ac.grim.grimac.utils.anticheat.MessageUtil;
import ac.grim.grimac.utils.math.Vec2d;
import ac.grim.grimac.utils.math.Vector3dm;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.util.Objects;

public class VectorData {
    public final VectorType vectorType;
    public VectorData lastVector;
    public VectorData preUncertainty;
    public double vectorX, vectorY, vectorZ;
    public @MonotonicNonNull Vec2d input;

    @Getter
    private boolean isKnockback, firstBreadKb, isExplosion, firstBreadExplosion, isTrident, isZeroPointZeroThree, isSwimHop, isFlipSneaking, isFlipItem, isJump, isAttackSlow = false;

    // For handling replacing the type of vector it is while keeping data
    public VectorData(Vector3dm vector, VectorData lastVector, VectorType vectorType) {
        this(vector.getX(), vector.getY(), vector.getZ(), lastVector, vectorType);
    }

    public void setX(double x) {
        this.vectorX = x;
    }

    public void setY(double y) {
        this.vectorY = y;
    }

    public void setZ(double z) {
        this.vectorZ = z;
    }

    public VectorData(final double x, final double y, final double z, VectorData lastVector, VectorType vectorType) {
        this.vectorX = x;
        this.vectorY = y;
        this.vectorZ = z;
        this.lastVector = lastVector;
        this.vectorType = vectorType;

        if (lastVector != null) {
            isKnockback = lastVector.isKnockback;
            firstBreadKb = lastVector.firstBreadKb;
            isExplosion = lastVector.isExplosion;
            firstBreadExplosion = lastVector.firstBreadExplosion;
            isTrident = lastVector.isTrident;
            isZeroPointZeroThree = lastVector.isZeroPointZeroThree;
            isSwimHop = lastVector.isSwimHop;
            isFlipSneaking = lastVector.isFlipSneaking;
            isFlipItem = lastVector.isFlipItem;
            isJump = lastVector.isJump;
            preUncertainty = lastVector.preUncertainty;
            isAttackSlow = lastVector.isAttackSlow;
            input = lastVector.input;
        }

        addVectorType(vectorType);
    }

    public VectorData(Vector3dm vector, VectorType vectorType) {
        this(vector.getX(), vector.getY(), vector.getZ(), vectorType);
    }

    public VectorData(final double x, final double y, final double z, VectorType vectorType) {
        this.vectorX = x;
        this.vectorY = y;
        this.vectorZ = z;
        this.vectorType = vectorType;
        addVectorType(vectorType);
    }

    public VectorData returnNewModified(VectorType type) {
        return new VectorData(this.vectorX, this.vectorY, this.vectorZ, this, type);
    }

    public VectorData returnNewModified(Vector3dm newVec, VectorType type) {
        return new VectorData(newVec, this, type);
    }

    public VectorData returnNewModified(final double x, final double y, final double z, VectorType type) {
        return new VectorData(x, y, z, this, type);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VectorData that = (VectorData) o;
        return isKnockback == that.isKnockback && firstBreadKb == that.firstBreadKb && isExplosion == that.isExplosion && firstBreadExplosion == that.firstBreadExplosion && isTrident == that.isTrident && isZeroPointZeroThree == that.isZeroPointZeroThree && isSwimHop == that.isSwimHop && isFlipSneaking == that.isFlipSneaking && isFlipItem == that.isFlipItem && isJump == that.isJump && isAttackSlow == that.isAttackSlow && vectorType == that.vectorType && Objects.equals(lastVector, that.lastVector) && Objects.equals(preUncertainty, that.preUncertainty) && vectorX == that.vectorX && vectorY == that.vectorY && vectorZ == that.vectorZ;
    }

    @Override
    public int hashCode() {
        return Objects.hash(vectorType, lastVector, preUncertainty, vectorX, vectorY, vectorZ, isKnockback, firstBreadKb, isExplosion, firstBreadExplosion, isTrident, isZeroPointZeroThree, isSwimHop, isFlipSneaking, isFlipItem, isJump, isAttackSlow);
    }

    public void addVectorType(VectorType type) {
        switch (type) {
            case Knockback -> isKnockback = true;
            case FirstBreadKnockback -> firstBreadKb = true;
            case Explosion -> isExplosion = true;
            case FirstBreadExplosion -> firstBreadExplosion = true;
            case Trident -> isTrident = true;
            case ZeroPointZeroThree -> isZeroPointZeroThree = true;
            case Swimhop -> isSwimHop = true;
            case Flip_Sneaking -> isFlipSneaking = true;
            case Flip_Use_Item -> isFlipItem = true;
            case Jump -> isJump = true;
            case AttackSlow -> isAttackSlow = true;
        }
    }

    @Override
    public String toString() {
        return "VectorData{" +
                "pointThree=" + isZeroPointZeroThree +
                ", vector=" + MessageUtil.toUnlabeledString(vectorX, vectorY, vectorZ) +
                '}';
    }

    // TODO: This is a stupid idea that slows everything down, remove it! There are easier ways to debug grim.
    // Would make false positives really easy to fix
    // But seriously, we could trace the code to find the mistake
    public enum VectorType {
        Normal,
        Swimhop,
        Climbable,
        Knockback,
        FirstBreadKnockback,
        HackyClimbable,
        Teleport,
        SkippedTicks,
        Explosion,
        FirstBreadExplosion,
        InputResult,
        StuckMultiplier,
        Spectator,
        Dead,
        Jump,
        SurfaceSwimming,
        SwimmingSpace,
        BestVelPicked,
        Firework,
        Lenience,
        TridentJump,
        Trident,
        SlimePistonBounce,
        ZeroPointZeroThree,
        AttackSlow,
        Flip_Sneaking,
        Flip_Use_Item,
        EntityPushing
    }
}
