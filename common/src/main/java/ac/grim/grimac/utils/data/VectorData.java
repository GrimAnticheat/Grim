package ac.grim.grimac.utils.data;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.math.Vector3dm;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.util.Objects;

public class VectorData {
    public final VectorType vectorType;
    public VectorData lastVector;
    public VectorData preUncertainty;
    public Vector3dm vector;
    public @MonotonicNonNull Vector3dm input;

    @Getter
    private boolean isKnockback, firstBreadKb, isExplosion, firstBreadExplosion, isTrident, isZeroPointZeroThree, isSwimHop, isFlipSneaking, isFlipItem, isJump, isAttackSlow = false;

    // For handling replacing the type of vector it is while keeping data
    public VectorData(Vector3dm vector, VectorData lastVector, VectorType vectorType) {
        this.vector = vector;
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
        this.vector = vector;
        this.vectorType = vectorType;
        addVectorType(vectorType);
    }

    public VectorData returnNewModified(VectorType type) {
        return new VectorData(vector, this, type);
    }

    public VectorData returnNewModified(Vector3dm newVec, VectorType type) {
        return new VectorData(newVec, this, type);
    }

    public boolean isSetbackKb(GrimPlayer player) {
        if (!isKnockback) {
            return false;
        }
        VelocityData bread = (firstBreadKb ? player.firstBreadKB : player.likelyKB);
        return bread != null && bread.isSetback;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VectorData that = (VectorData) o;
        return isKnockback == that.isKnockback && firstBreadKb == that.firstBreadKb && isExplosion == that.isExplosion && firstBreadExplosion == that.firstBreadExplosion && isTrident == that.isTrident && isZeroPointZeroThree == that.isZeroPointZeroThree && isSwimHop == that.isSwimHop && isFlipSneaking == that.isFlipSneaking && isFlipItem == that.isFlipItem && isJump == that.isJump && isAttackSlow == that.isAttackSlow && vectorType == that.vectorType && Objects.equals(lastVector, that.lastVector) && Objects.equals(preUncertainty, that.preUncertainty) && Objects.equals(vector, that.vector);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vectorType, lastVector, preUncertainty, vector, isKnockback, firstBreadKb, isExplosion, firstBreadExplosion, isTrident, isZeroPointZeroThree, isSwimHop, isFlipSneaking, isFlipItem, isJump, isAttackSlow);
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
                ", vector=" + vector +
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
