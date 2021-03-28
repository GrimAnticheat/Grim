package org.abyssmc.reaperac.checks.movement.predictions;

import net.minecraft.server.v1_16_R3.BlockPosition;
import org.abyssmc.reaperac.GrimPlayer;
import org.abyssmc.reaperac.utils.math.MovementVectorsCalc;
import org.abyssmc.reaperac.utils.nmsImplementations.FluidFallingAdjustedMovement;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class PredictionEngineWater extends PredictionEngine {
    boolean isFalling;
    double playerGravity;
    float swimmingSpeed;
    float swimmingFriction;
    double lastY;

    public void guessBestMovement(float swimmingSpeed, GrimPlayer grimPlayer, boolean isFalling, double playerGravity, float swimmingFriction, double lastY) {
        this.isFalling = isFalling;
        this.playerGravity = playerGravity;
        this.swimmingSpeed = swimmingFriction;
        this.swimmingFriction = swimmingFriction;
        this.lastY = lastY;
        super.guessBestMovement(swimmingSpeed, grimPlayer);
    }

    @Override
    public void addJumpIfNeeded(GrimPlayer grimPlayer) {
        grimPlayer.clientVelocityJumping = grimPlayer.clientVelocity.clone().add(new Vector(0, 0.04, 0));
        super.addJumpIfNeeded(grimPlayer);
    }

    @Override
    public List<Vector> fetchPossibleInputs(GrimPlayer grimPlayer) {
        List<Vector> velocities = grimPlayer.getPossibleVelocities();
        List<Vector> swimmingVelocities = new ArrayList<>();

        if (grimPlayer.bukkitPlayer.isSwimming() && grimPlayer.bukkitPlayer.getVehicle() == null) {
            for (Vector vector : velocities) {
                double d5;
                double d = MovementVectorsCalc.getLookAngle(grimPlayer).y;
                d5 = d < -0.2 ? 0.085 : 0.06;

                // if (d3 <= 0.0D || this.isJumping || !this.world.getBlockState(new BlockPos(this.getPosX(), this.getPosY() + 1.0D - 0.1D, this.getPosZ())).getFluidState().isEmpty()) {
                // If the player is looking upward
                // I removed the isJumping check and everything works fine
                // This is most likely due to the player not swimming if they are not jumping in the other two scenarios
                if (d <= 0.0 || !((CraftWorld) grimPlayer.bukkitPlayer.getWorld()).getHandle().getFluid(new BlockPosition(grimPlayer.lastX, grimPlayer.lastY + 1.0 - 0.1, grimPlayer.lastZ)).isEmpty()) {
                    swimmingVelocities.add(new Vector(vector.getX(), vector.getY() + ((d - vector.getY()) * d5), vector.getZ()));
                }
            }

            return swimmingVelocities;
        }

        return velocities;
    }

    @Override
    public void endOfTick(GrimPlayer grimPlayer, double playerGravity, float friction) {
        for (Vector vector : grimPlayer.getPossibleVelocitiesMinusKnockback()) {
            vector.multiply(new Vector(swimmingFriction, 0.8F, swimmingFriction));
            Vector fluidVector = FluidFallingAdjustedMovement.getFluidFallingAdjustedMovement(grimPlayer, playerGravity, isFalling, vector);
            vector.setX(fluidVector.getX());
            vector.setY(fluidVector.getY());
            vector.setZ(fluidVector.getZ());
        }
    }
}
