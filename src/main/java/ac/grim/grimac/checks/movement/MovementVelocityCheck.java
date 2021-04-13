package ac.grim.grimac.checks.movement;

import ac.grim.grimac.GrimPlayer;
import ac.grim.grimac.checks.movement.predictions.PredictionEngineLava;
import ac.grim.grimac.checks.movement.predictions.PredictionEngineNormal;
import ac.grim.grimac.checks.movement.predictions.PredictionEngineWater;
import ac.grim.grimac.utils.data.FireworkData;
import ac.grim.grimac.utils.enums.FluidTag;
import ac.grim.grimac.utils.enums.MoverType;
import ac.grim.grimac.utils.math.MovementVectorsCalc;
import ac.grim.grimac.utils.math.Mth;
import ac.grim.grimac.utils.nmsImplementations.BlockProperties;
import ac.grim.grimac.utils.nmsImplementations.Collisions;
import ac.grim.grimac.utils.nmsImplementations.FluidFallingAdjustedMovement;
import net.minecraft.server.v1_16_R3.EnchantmentManager;
import net.minecraft.server.v1_16_R3.EntityPlayer;
import net.minecraft.server.v1_16_R3.MathHelper;
import net.minecraft.server.v1_16_R3.MobEffects;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class MovementVelocityCheck {
    private final Player bukkitPlayer;
    private final GrimPlayer grimPlayer;

    public MovementVelocityCheck(GrimPlayer grimPlayer) {
        this.grimPlayer = grimPlayer;
        this.bukkitPlayer = grimPlayer.bukkitPlayer;
    }

    // Entity line 527
    // TODO: Entity piston and entity shulker (want to) call this method too.
    public static Vector move(GrimPlayer grimPlayer, MoverType moverType, Vector vec3) {
        // Something about noClip
        // Piston movement exemption
        // What is a motion multiplier?
        Vector stuckSpeedMultiplier = grimPlayer.stuckSpeedMultiplier;

        if (stuckSpeedMultiplier.getX() < 0.99) {
            vec3 = vec3.multiply(stuckSpeedMultiplier);
            grimPlayer.baseTickSetX(0);
            grimPlayer.baseTickSetY(0);
            grimPlayer.baseTickSetZ(0);
        }

        Vector clonedClientVelocity = Collisions.collide(Collisions.maybeBackOffFromEdge(vec3, moverType, grimPlayer), grimPlayer);

        if (stuckSpeedMultiplier.getX() < 0.99) {
            vec3 = vec3.multiply(stuckSpeedMultiplier);
            clonedClientVelocity = new Vector();
        }

        grimPlayer.horizontalCollision = !Mth.equal(vec3.getX(), clonedClientVelocity.getX()) || !Mth.equal(vec3.getZ(), clonedClientVelocity.getZ());
        grimPlayer.verticalCollision = vec3.getY() != clonedClientVelocity.getY();

        grimPlayer.predictedVelocity = clonedClientVelocity.clone();

        if (vec3.getX() != clonedClientVelocity.getX()) {
            clonedClientVelocity.setX(0);
        }

        if (vec3.getZ() != clonedClientVelocity.getZ()) {
            clonedClientVelocity.setZ(0);
        }

        Location getBlockLocation;

        getBlockLocation = new Location(grimPlayer.playerWorld, grimPlayer.x, grimPlayer.y - 0.2F, grimPlayer.z);

        Block onBlock = BlockProperties.getOnBlock(getBlockLocation);

        if (vec3.getY() != clonedClientVelocity.getY()) {
            if (onBlock.getType() == org.bukkit.Material.SLIME_BLOCK) {
                // TODO: Maybe lag compensate this (idk packet order)
                if (grimPlayer.isSneaking) {
                    clonedClientVelocity.setY(0);
                } else {
                    if (clonedClientVelocity.getY() < 0.0) {
                        clonedClientVelocity.setY(-vec3.getY());
                    }
                }
            } else if (onBlock.getBlockData() instanceof Bed) {
                if (clonedClientVelocity.getY() < 0.0) {
                    clonedClientVelocity.setY(-vec3.getY() * 0.6600000262260437);
                }
            } else {
                clonedClientVelocity.setY(0);
            }
        }

        float f = BlockProperties.getBlockSpeedFactor(grimPlayer);
        clonedClientVelocity.multiply(new Vector(f, 1.0, f));

        return clonedClientVelocity;
    }

    public void livingEntityAIStep() {
        // Living Entity line 2153
        // TODO: 1.8 clients have a different minimum movement than 1.9.  I believe it is 0.005
        for (Vector vector : grimPlayer.getPossibleVelocitiesMinusKnockback()) {
            if (Math.abs(vector.getX()) < 0.003D) {
                vector.setX(0D);
            }

            if (Math.abs(vector.getY()) < 0.003D) {
                vector.setY(0D);
            }

            if (Math.abs(vector.getZ()) < 0.003D) {
                vector.setZ(0D);
            }
        }

        playerEntityTravel();
    }

    // Player line 1208
    public void playerEntityTravel() {
        grimPlayer.clientVelocitySwimHop = null;

        if (grimPlayer.isFlying && grimPlayer.bukkitPlayer.getVehicle() == null) {
            double oldY = grimPlayer.clientVelocity.getY();
            double oldYJumping = grimPlayer.clientVelocityJumping.getY();
            livingEntityTravel();

            if (Math.abs(oldY - grimPlayer.actualMovement.getY()) < (oldYJumping - grimPlayer.actualMovement.getY())) {
                grimPlayer.baseTickSetY(oldY * 0.6);

            } else {
                grimPlayer.baseTickSetY(oldYJumping * 0.6);

            }

        } else {
            livingEntityTravel();
        }

        grimPlayer.clientVelocityJumping = null;
        grimPlayer.clientVelocityFireworkBoost = null;
    }

    // LivingEntity line 1741
    public void livingEntityTravel() {
        double playerGravity = 0.08;

        // TODO: Stop being lazy and rename these variables to be descriptive
        boolean isFalling = grimPlayer.clientVelocity.getY() <= 0.0;
        if (isFalling && grimPlayer.bukkitPlayer.hasPotionEffect(PotionEffectType.SLOW_FALLING)) {
            playerGravity = 0.01;
            //this.fallDistance = 0.0f;
        }

        grimPlayer.gravity = playerGravity;

        EntityPlayer entityPlayer = grimPlayer.entityPlayer;

        double lastY;
        float swimFriction;
        float f2;

        handleFireworks();

        if (grimPlayer.wasTouchingWater && !grimPlayer.entityPlayer.abilities.isFlying) {
            // 0.8F seems hardcoded in
            lastY = grimPlayer.lastY;
            swimFriction = entityPlayer.isSprinting() ? 0.9F : 0.8F;
            float swimSpeed = 0.02F;
            f2 = (float) EnchantmentManager.e(entityPlayer);
            if (f2 > 3.0F) {
                f2 = 3.0F;
            }

            if (!grimPlayer.lastOnGround) {
                f2 *= 0.5F;
            }

            if (f2 > 0.0F) {
                swimFriction += (0.54600006F - swimFriction) * f2 / 3.0F;
                swimSpeed += (entityPlayer.dN() - swimSpeed) * f2 / 3.0F;
            }

            if (entityPlayer.hasEffect(MobEffects.DOLPHINS_GRACE)) {
                swimFriction = 0.96F;
            }

            new PredictionEngineWater().guessBestMovement(swimSpeed, grimPlayer, isFalling, playerGravity, swimFriction, lastY);

            /*grimPlayer.clientVelocityOnLadder = null;
            if (grimPlayer.lastClimbing) {
                grimPlayer.clientVelocityOnLadder = endOfTickWaterMovement(grimPlayer.clientVelocity.clone().setY(0.2), bl, d, f, d1);
            }

            grimPlayer.clientVelocity = endOfTickWaterMovement(grimPlayer.clientVelocity, bl, d, f, d1);*/

        } else {
            if (grimPlayer.fluidHeight.getOrDefault(FluidTag.LAVA, 0) > 0 && !grimPlayer.entityPlayer.abilities.isFlying) {
                lastY = grimPlayer.lastY;

                new PredictionEngineLava().guessBestMovement(0.02F, grimPlayer);

                if (grimPlayer.fluidHeight.getOrDefault(FluidTag.LAVA, 0) <= 0.4D) {
                    grimPlayer.clientVelocity = grimPlayer.clientVelocity.multiply(new Vector(0.5D, 0.800000011920929D, 0.5D));
                    grimPlayer.clientVelocity = FluidFallingAdjustedMovement.getFluidFallingAdjustedMovement(grimPlayer, playerGravity, isFalling, grimPlayer.clientVelocity);
                } else {
                    grimPlayer.clientVelocity.multiply(0.5D);
                }

                // Removed reference to gravity
                grimPlayer.clientVelocity.add(new Vector(0.0D, -playerGravity / 4.0D, 0.0D));

                if (grimPlayer.horizontalCollision && entityPlayer.e(grimPlayer.clientVelocity.getX(), grimPlayer.clientVelocity.getY() + 0.6000000238418579D - grimPlayer.y + lastY, grimPlayer.clientVelocity.getZ())) {
                    grimPlayer.clientVelocity = new Vector(grimPlayer.clientVelocity.getX(), 0.30000001192092896D, grimPlayer.clientVelocity.getZ());
                }

            } else if (bukkitPlayer.isGliding()) {
                Vector clientVelocity = grimPlayer.clientVelocity.clone();

                double bestMovement = Double.MAX_VALUE;
                for (Vector possibleVelocity : grimPlayer.getPossibleVelocities()) {
                    possibleVelocity = getElytraMovement(possibleVelocity.clone(), MovementVectorsCalc.getVectorForRotation(grimPlayer.yRot, grimPlayer.xRot));
                    double closeness = possibleVelocity.distanceSquared(grimPlayer.actualMovement);

                    if (closeness < bestMovement) {
                        bestMovement = closeness;
                        clientVelocity = possibleVelocity;
                    }
                }

                grimPlayer.clientVelocity = clientVelocity;

                grimPlayer.clientVelocity.multiply(new Vector(0.99F, 0.98F, 0.99F));
                grimPlayer.predictedVelocity = grimPlayer.clientVelocity.clone();
                grimPlayer.clientVelocity = move(grimPlayer, MoverType.SELF, grimPlayer.clientVelocity);

            } else {
                float blockFriction = BlockProperties.getBlockFriction(grimPlayer);
                float f6 = grimPlayer.lastOnGround ? blockFriction * 0.91f : 0.91f;
                grimPlayer.friction = f6;

                new PredictionEngineNormal().guessBestMovement(BlockProperties.getFrictionInfluencedSpeed(blockFriction, grimPlayer), grimPlayer);
            }
        }
    }

    public void handleFireworks() {
        int maxFireworks = grimPlayer.fireworks.size();
        Vector lookVector = MovementVectorsCalc.getVectorForRotation(grimPlayer.yRot, grimPlayer.xRot);

        if (maxFireworks > 0) {
            grimPlayer.clientVelocityFireworkBoost = grimPlayer.clientVelocity.clone();

            while (maxFireworks-- > 0) {
                Vector anotherBoost = grimPlayer.clientVelocityFireworkBoost.clone().add(new Vector(lookVector.getX() * 0.1 + (lookVector.getX() * 1.5 - grimPlayer.clientVelocityFireworkBoost.getX()) * 0.5, lookVector.getY() * 0.1 + (lookVector.getY() * 1.5 - grimPlayer.clientVelocityFireworkBoost.getY()) * 0.5, (lookVector.getZ() * 0.1 + (lookVector.getZ() * 1.5 - grimPlayer.clientVelocityFireworkBoost.getZ()) * 0.5)));

                if (getElytraMovement(anotherBoost.clone(), lookVector).distanceSquared(grimPlayer.actualMovement) < getElytraMovement(grimPlayer.clientVelocityFireworkBoost.clone(), lookVector).distanceSquared(grimPlayer.actualMovement)) {
                    grimPlayer.clientVelocityFireworkBoost = anotherBoost;
                } else {
                    maxFireworks++;
                    break;
                }
            }
        }

        int usedFireworks = grimPlayer.fireworks.size() - maxFireworks;

        for (FireworkData data : grimPlayer.fireworks.values()) {
            if (data.hasApplied) {
                usedFireworks--;
            }
        }

        while (usedFireworks-- > 0) {
            for (FireworkData data : grimPlayer.fireworks.values()) {
                if (!data.hasApplied) {
                    data.setApplied();
                    usedFireworks--;
                }
            }
        }

        // Do this last to give an extra 50 ms of buffer on top of player ping
        grimPlayer.fireworks.entrySet().removeIf(entry -> entry.getValue().getLagCompensatedDestruction() < System.nanoTime());
    }

    public Vector getElytraMovement(Vector vector, Vector lookVector) {
        float yRotRadians = grimPlayer.yRot * 0.017453292F;
        double d2 = Math.sqrt(lookVector.getX() * lookVector.getX() + lookVector.getZ() * lookVector.getZ());
        double d3 = vector.clone().setY(0).length();
        double d4 = lookVector.length();
        float f3 = MathHelper.cos(yRotRadians);
        f3 = (float) ((double) f3 * (double) f3 * Math.min(1.0D, d4 / 0.4D));
        vector.add(new Vector(0.0D, grimPlayer.gravity * (-1.0D + (double) f3 * 0.75D), 0.0D));
        double d5;
        if (vector.getY() < 0.0D && d2 > 0.0D) {
            d5 = vector.getY() * -0.1D * (double) f3;
            vector.add(new Vector(lookVector.getX() * d5 / d2, d5, lookVector.getZ() * d5 / d2));
        }

        if (yRotRadians < 0.0F && d2 > 0.0D) {
            d5 = d3 * (double) (-MathHelper.sin(yRotRadians)) * 0.04D;
            vector.add(new Vector(-lookVector.getX() * d5 / d2, d5 * 3.2D, -lookVector.getZ() * d5 / d2));
        }

        if (d2 > 0) {
            vector.add(new Vector((lookVector.getX() / d2 * d3 - vector.getX()) * 0.1D, 0.0D, (lookVector.getZ() / d2 * d3 - vector.getZ()) * 0.1D));
        }

        return vector;
    }
}