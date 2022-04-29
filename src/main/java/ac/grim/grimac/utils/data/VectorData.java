package ac.grim.grimac.utils.data;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.predictionengine.predictions.PredictionEngineNormal;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.math.GrimMath;
import ac.grim.grimac.utils.nmsutil.Collisions;
import com.google.common.base.Objects;
import lombok.Getter;
import org.bukkit.util.Vector;

public class VectorData {
    public VectorType vectorType;
    public VectorData lastVector;
    public Vector vector;
    private static final double saneMaxInput = 1.3; // Sprinting bonus

    @Getter
    private boolean isKnockback, isExplosion, isTrident, isZeroPointZeroThree, isSwimHop, isFlipSneaking, isJump = false;
    public Vector playerInputs, actualMovement, clientVelBeforeMovement;

    public VectorData(Vector vector, VectorType vectorType) {
        this.vector = vector;
        this.vectorType = vectorType;
        addVectorType(vectorType);
    }

    public VectorData returnNewModified(Vector newVec, VectorType type) {
        return new VectorData(newVec, this, type);
    }


    @Override
    public int hashCode() {
        return Objects.hashCode(vectorType, vector, isKnockback, isExplosion, isTrident, isZeroPointZeroThree, isSwimHop, isFlipSneaking, isJump);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VectorData that = (VectorData) o;
        return isKnockback == that.isKnockback && isExplosion == that.isExplosion && isTrident == that.isTrident && isZeroPointZeroThree == that.isZeroPointZeroThree && isSwimHop == that.isSwimHop && isFlipSneaking == that.isFlipSneaking && isJump == that.isJump && Objects.equal(vector, that.vector);
    }

    private void addVectorType(VectorType type) {
        switch (type) {
            case Knockback:
                isKnockback = true;
                break;
            case Explosion:
                isExplosion = true;
                break;
            case Trident:
                isTrident = true;
                break;
            case ZeroPointZeroThree:
                isZeroPointZeroThree = true;
                break;
            case Swimhop:
                isSwimHop = true;
                break;
            case Flip_Sneaking:
                isFlipSneaking = true;
                break;
            case Jump:
                isJump = true;
                break;
        }
    }

    // For handling replacing the type of vector it is while keeping data
    public VectorData(Vector vector, VectorData lastVector, VectorType vectorType) {
        this.vector = vector;
        this.lastVector = lastVector;
        this.vectorType = vectorType;

        if (lastVector != null) {
            isKnockback = lastVector.isKnockback;
            isExplosion = lastVector.isExplosion;
            isTrident = lastVector.isTrident;
            isZeroPointZeroThree = lastVector.isZeroPointZeroThree;
            isSwimHop = lastVector.isSwimHop;
            isFlipSneaking = lastVector.isFlipSneaking;
            isJump = lastVector.isJump;
            playerInputs = lastVector.playerInputs;
            actualMovement = lastVector.actualMovement;
            clientVelBeforeMovement = lastVector.clientVelBeforeMovement;
        }

        addVectorType(vectorType);
    }

    // These math equations are based off of the vanilla equations, made impossible to divide by 0
    public static Vector getBestTheoreticalPlayerInput(GrimPlayer player, Vector wantedMovement, float f, float f2) {
        float f3 = player.trigHandler.sin(f2 * 0.017453292f);
        float f4 = player.trigHandler.cos(f2 * 0.017453292f);

        float bestTheoreticalX = (float) (f3 * wantedMovement.getZ() + f4 * wantedMovement.getX()) / (f3 * f3 + f4 * f4) / f;
        float bestTheoreticalZ = (float) (-f3 * wantedMovement.getX() + f4 * wantedMovement.getZ()) / (f3 * f3 + f4 * f4) / f;

        return new Vector(bestTheoreticalX, 0, bestTheoreticalZ);
    }

    public Vector storeAndSanifyVector(GrimPlayer player, float speed, Vector actualMovement, Vector clientVelBeforeMovement) {
        // Alright, so we can't limit the inputs themselves because pressing W moves both in the X and Z axis
        // So we have to limit the axis individually, so that collisions in an axis does not affect the other axis
        double maxSimulatedMovement = speed * saneMaxInput;
        Vector clientClaimedMovement = actualMovement.clone().subtract(clientVelBeforeMovement);
        Vector simulated = new Vector(GrimMath.clamp(clientClaimedMovement.getX(), -maxSimulatedMovement, maxSimulatedMovement), 0, GrimMath.clamp(clientClaimedMovement.getZ(), -maxSimulatedMovement, maxSimulatedMovement));

        Vector theoreticalInput = getBestTheoreticalPlayerInput(player, simulated, speed, player.xRot);

        double bestPossibleX = GrimMath.clamp(theoreticalInput.getX(), -saneMaxInput, saneMaxInput);
        double bestPossibleZ = GrimMath.clamp(theoreticalInput.getZ(), -saneMaxInput, saneMaxInput);

        this.actualMovement = actualMovement;
        this.clientVelBeforeMovement = clientVelBeforeMovement;
        return playerInputs = new Vector(bestPossibleX, 0, bestPossibleZ);
    }

    public InputsOffsetData calculateOffset(GrimPlayer player, boolean forceSneaking, boolean forceUseItem, float speed) {
        double minimumOffset = Double.MAX_VALUE;
        Vector playerInputs = null;
        boolean bestSprinting = false;
        boolean bestUseItem = false;
        boolean bestSneaking = false;

        double epsilon = SimpleCollisionBox.COLLISION_EPSILON;
        boolean posXColl = player.predictedVelocity.vector.getX() > epsilon && player.xAxisCollision;
        boolean negXColl = player.predictedVelocity.vector.getX() < -epsilon && player.xAxisCollision;
        boolean posZColl = player.predictedVelocity.vector.getZ() > epsilon && player.zAxisCollision;
        boolean negZColl = player.predictedVelocity.vector.getZ() < -epsilon && player.zAxisCollision;


        // This may look stupid, but it takes 0.005 milliseconds, so it's not that bad.
        for (int shouldCheckCollisions = 0; shouldCheckCollisions < 2; shouldCheckCollisions++) {
            for (int isSprinting = 0; isSprinting < 2; isSprinting++) {
                float speedWithSprinting = isSprinting == 1 ? speed += speed * 0.3f : speed;

                if (shouldCheckCollisions == 1) {
                    // Check for the player walking against a block without actually moving at all
                    // (This is a waste of compute if they aren't, so try to calculate it without it)
                    if (player.actualMovement.getX() != 0 || player.actualMovement.getZ() != 0) {
                        if (player.actualMovement.getX() > -epsilon && player.actualMovement.getX() < epsilon) {
                            posXColl = posXColl | Collisions.collide(player, epsilon, 0, 0).getX() != epsilon;
                            negXColl = negXColl | Collisions.collide(player, -epsilon, 0, 0).getX() != -epsilon;
                        }

                        if (player.actualMovement.getZ() > -epsilon && player.actualMovement.getZ() < epsilon) {
                            posZColl = posZColl | Collisions.collide(player, 0, 0, epsilon).getZ() != epsilon;
                            negZColl = negZColl | Collisions.collide(player, 0, 0, -epsilon).getZ() != -epsilon;
                        }
                    } else {
                        break; // Well... we didn't check for collisions so nothing changed.
                    }
                }

                for (double x = -1; x < 2; x++) {
                    for (double z = -1; z < 2; z++) {
                        for (int isUsingItem = forceUseItem ? 1 : 0; isUsingItem < 2; isUsingItem++) {
                            for (int isSneaking = forceSneaking ? 1 : 0; isSneaking < 2; isSneaking++) {
                                Vector toInput = transformInputsToVector(isUsingItem == 1, isSneaking == 1, new Vector(x, 0, z));
                                // This is a lot of trig calls, but there's a lookup table and a CPU cache :)
                                Vector simulated = clientVelBeforeMovement.clone().add(new PredictionEngineNormal().getMovementResultFromInput(player, toInput, speedWithSprinting, player.xRot)).setY(0);
                                Vector simulatedFakeCollision = simulated.clone();

                                // If the player collided while going towards positive X, and the input vector is greater than
                                // the movement after the collision, meaning it would have also made the collision and
                                // would have been limited to the same value.  Then we set it to the collision value
                                if (posXColl) {
                                    simulatedFakeCollision.setX(player.predictedVelocity.vector.getX());
                                }
                                // Ditto for negative X
                                else if (negXColl) {
                                    simulatedFakeCollision.setX(player.predictedVelocity.vector.getX());
                                }

                                // ditto positive Z
                                if (posZColl) {
                                    simulatedFakeCollision.setZ(player.predictedVelocity.vector.getZ());
                                }
                                // ditto negative Z
                                else if (negZColl) {
                                    simulatedFakeCollision.setZ(player.predictedVelocity.vector.getZ());
                                }

                                double offset = simulatedFakeCollision.distanceSquared(player.actualMovement.clone().setY(0));
                                offset = Math.max(offset, 0);

                                if (offset < minimumOffset) {
                                    minimumOffset = offset;
                                    playerInputs = toInput;
                                    bestSprinting = isSprinting == 1;
                                    bestUseItem = isUsingItem == 1;
                                    bestSneaking = isSneaking == 1;
                                }

                                if (minimumOffset < 0.0001 * 0.0001) {
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }

        return new InputsOffsetData(playerInputs, minimumOffset, bestSprinting, bestUseItem, bestSneaking);
    }

    // Remember, sneaking is *= 0.3D while use item is *= 0.2F
    private Vector transformInputsToVector(boolean sneaking, boolean useItem, Vector theoreticalInput) {
        float bestPossibleX;
        float bestPossibleZ;

        // Slow movement was determined by the previous pose
        if (sneaking) {
            bestPossibleX = (float) (Math.min(Math.max(-1f, Math.round(theoreticalInput.getX() / 0.3)), 1f) * 0.3d);
            bestPossibleZ = (float) (Math.min(Math.max(-1f, Math.round(theoreticalInput.getZ() / 0.3)), 1f) * 0.3d);
        } else {
            bestPossibleX = Math.min(Math.max(-1f, Math.round(theoreticalInput.getX())), 1f);
            bestPossibleZ = Math.min(Math.max(-1f, Math.round(theoreticalInput.getZ())), 1f);
        }

        if (useItem) {
            bestPossibleX *= 0.2F;
            bestPossibleZ *= 0.2F;
        }

        Vector inputVector = new Vector(bestPossibleX, 0, bestPossibleZ);
        inputVector.multiply(0.98F);

        // Simulate float rounding imprecision
        inputVector = new Vector((float) inputVector.getX(), (float) inputVector.getY(), (float) inputVector.getZ());

        if (inputVector.lengthSquared() > 1) {
            double d0 = Math.sqrt(inputVector.getX() * inputVector.getX() + inputVector.getY() * inputVector.getY() + inputVector.getZ() * inputVector.getZ());
            inputVector = new Vector(inputVector.getX() / d0, inputVector.getY() / d0, inputVector.getZ() / d0);
        }

        return inputVector;
    }

    @Override
    public String toString() {
        return "VectorData{" +
                "vectorType=" + vectorType +
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
        HackyClimbable,
        Teleport,
        SkippedTicks,
        Explosion,
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
        Entity_Pushing,
        ZeroPointZeroThree,
        AttackSlow,
        Flip_Sneaking
    }
}
