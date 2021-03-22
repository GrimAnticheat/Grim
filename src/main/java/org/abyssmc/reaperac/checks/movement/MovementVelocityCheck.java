package org.abyssmc.reaperac.checks.movement;

import net.minecraft.server.v1_16_R3.*;
import org.abyssmc.reaperac.GrimPlayer;
import org.abyssmc.reaperac.ReaperAC;
import org.abyssmc.reaperac.checks.movement.predictions.NormalPrediction;
import org.abyssmc.reaperac.checks.movement.predictions.WithLadderPrediction;
import org.abyssmc.reaperac.events.anticheat.PlayerBaseTick;
import org.abyssmc.reaperac.utils.enums.FluidTag;
import org.abyssmc.reaperac.utils.enums.MoverType;
import org.abyssmc.reaperac.utils.math.MovementVectorsCalc;
import org.abyssmc.reaperac.utils.math.Mth;
import org.abyssmc.reaperac.utils.nmsImplementations.BlockProperties;
import org.abyssmc.reaperac.utils.nmsImplementations.Collisions;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class MovementVelocityCheck implements Listener {
    private Player bukkitPlayer;
    private GrimPlayer grimPlayer;

    @EventHandler
    public void onPlayerMoveEvent(PlayerMoveEvent event) {
        this.bukkitPlayer = event.getPlayer();
        this.grimPlayer = ReaperAC.playerGrimHashMap.get(bukkitPlayer);
        grimPlayer.movementEventMilliseconds = System.currentTimeMillis();

        Location from = event.getFrom();
        Location to = event.getTo();

        grimPlayer.lastTickPosition = from;

        // TODO: LivingEntity: 1882 (fluid adjusted movement)

        // This isn't the final velocity of the player in the tick, only the one applied to the player
        grimPlayer.actualMovement = new Vector(to.getX() - from.getX(), to.getY() - from.getY(), to.getZ() - from.getZ());

        // To get the velocity of the player in the beginning of the next tick
        // We need to run the code that is ran after the movement is applied to the player
        // We do it at the start of the next movement check where the movement is applied
        // This allows the check to be more accurate than if we were a tick off on the player position
        //
        // Currently disabled because I'd rather know if something is wrong than try and hide it
        //grimPlayer.clientVelocity = move(MoverType.SELF, grimPlayer.lastActualMovement, false);

        // With 0 ping I haven't found ANY margin of error
        // Very useful for reducing x axis effect on y axis precision
        // Since the Y axis is extremely easy to predict
        // It once is different if the player is trying to clip through stuff
        grimPlayer.actualMovementCalculatedCollision = Collisions.collide(Collisions.maybeBackOffFromEdge(grimPlayer.actualMovement, MoverType.SELF, grimPlayer), grimPlayer);

        // This is not affected by any movement
        new PlayerBaseTick(grimPlayer).doBaseTick();

        // baseTick occurs before this
        livingEntityAIStep();

        ChatColor color;
        double diff = grimPlayer.predictedVelocity.distance(grimPlayer.actualMovement);

        if (diff < 0.05) {
            color = ChatColor.GREEN;
        } else if (diff < 0.15) {
            color = ChatColor.YELLOW;
        } else {
            color = ChatColor.RED;
        }

        Bukkit.broadcastMessage("Time since last event " + (grimPlayer.movementEventMilliseconds - grimPlayer.lastMovementEventMilliseconds));
        Bukkit.broadcastMessage("P: " + color + grimPlayer.predictedVelocity.getX() + " " + grimPlayer.predictedVelocity.getY() + " " + grimPlayer.predictedVelocity.getZ());
        Bukkit.broadcastMessage("A: " + color + grimPlayer.actualMovement.getX() + " " + grimPlayer.actualMovement.getY() + " " + grimPlayer.actualMovement.getZ());


        // TODO: This is a check for is the player actually on the ground!
        // TODO: This check is wrong with less 1.9+ precision on movement
        // mainly just debug for now rather than an actual check
        if (grimPlayer.isActuallyOnGround != grimPlayer.lastOnGround) {
            Bukkit.broadcastMessage("Failed on ground, client believes: " + grimPlayer.onGround);
        }

        if (grimPlayer.predictedVelocity.distanceSquared(grimPlayer.actualMovement) > new Vector(0.03, 0.03, 0.03).lengthSquared()) {
            //Bukkit.broadcastMessage(ChatColor.RED + "FAILED MOVEMENT CHECK");
        }

        grimPlayer.lastActualMovement = grimPlayer.actualMovement;
    }

    public void livingEntityAIStep() {
        // not sure if this is correct
        // Living Entity line 2153 (fuck, must have switched mappings)
        //clientVelocity.multiply(0.98f);

        // Living Entity line 2153
        // TODO: Extend this check so 1.8 clients don't trigger it
        if (Math.abs(grimPlayer.clientVelocity.getX()) < 0.003D) {
            grimPlayer.clientVelocity.setX(0D);
        }

        if (Math.abs(grimPlayer.clientVelocity.getY()) < 0.003D) {
            grimPlayer.clientVelocity.setY(0D);
        }

        if (Math.abs(grimPlayer.clientVelocity.getZ()) < 0.003D) {
            grimPlayer.clientVelocity.setZ(0D);
        }

        // Now it gets input
        // Now it does jumping and fluid movement

        // Living Entity line 2180
        // We moved this down after everything else is calculated
        //float sidewaysSpeed = 0f;
        //float forwardsSpeed = 1f;

        // random stuff about jumping in liquids
        // TODO: Jumping in liquids
        // We don't have an accurate way to know if the player is jumping, so this will do
        // This is inspired by paper's playerJumpEvent
        // LivingEntity line 2185

        /*if (grimPlayer.lastOnGround && !grimPlayer.onGround && grimPlayer.y > grimPlayer.lastY) {
        //if (this.jumping && this.isAffectedByFluids()) {
            double d7 = this.isInLava() ? this.getFluidHeight(FluidTags.LAVA) : this.getFluidHeight(FluidTags.WATER);
            boolean bl = this.isInWater() && d7 > 0.0;
            if (bl && (!this.onGround || d7 > fluidJumpThreshold)) {
                this.jumpInLiquid(FluidTags.WATER);
            } else if (this.isInLava() && (!this.onGround || d7 > fluidJumpThreshold)) {
                this.jumpInLiquid(FluidTags.LAVA);
            } else if ((this.onGround || bl && d7 <= fluidJumpThreshold) && this.noJumpDelay == 0) {
                this.jumpFromGround();
                this.noJumpDelay = 10;
            }
        } else {
            this.noJumpDelay = 0;
        }*/

        // Living Entity line 2202
        //sidewaysSpeed *= 0.98f;
        //forwardsSpeed *= 0.98f;

        //Vector inputVector = new Vector(sidewaysSpeed, 0, forwardsSpeed);

        // Living entity line 2206
        //livingEntityTravel(inputVector);

        //playerEntityTravel();
        livingEntityTravel();


        //clientVelocity.multiply(0.98f);
    }

    /*public void playerEntityTravel() {
        if (bukkitPlayer.isSwimming() && !bukkitPlayer.isInsideVehicle()) {
            double d3 = this.getLookAngle().y;
            double d4 = d3 < -0.2D ? 0.085D : 0.06D;
            if (d3 <= 0.0D || this.isJumping || !this.world.getBlockState(new BlockPos(this.getPosX(), this.getPosY() + 1.0D - 0.1D, this.getPosZ())).getFluidState().isEmpty()) {
                Vector3d vector3d1 = this.getMotion();
                this.setMotion(vector3d1.add(0.0D, (d3 - vector3d1.y) * d4, 0.0D));
            }
        }

        if (this.abilities.isFlying && !this.isPassenger()) {
            double d5 = this.getMotion().y;
            float f = this.jumpMovementFactor;
            this.jumpMovementFactor = this.abilities.getFlySpeed() * (float)(this.isSprinting() ? 2 : 1);
            super.travel(travelVector);
            Vector3d vector3d = this.getMotion();
            this.setMotion(vector3d.x, d5 * 0.6D, vector3d.z);
            this.jumpMovementFactor = f;
            this.fallDistance = 0.0F;
            this.setFlag(7, false);
        } else {
            super.travel(travelVector);
        }
    }*/

    // LivingEntity line 1741
    public void livingEntityTravel() {
        double d = 0.08;

        // TODO: Stop being lazy and rename these variables to be descriptive
        boolean bl = grimPlayer.clientVelocity.getY() <= 0.0;
        if (bl && grimPlayer.bukkitPlayer.hasPotionEffect(PotionEffectType.SLOW_FALLING)) {
            d = 0.01;
            //this.fallDistance = 0.0f;
        }

        EntityPlayer entityPlayer = grimPlayer.entityPlayer;
        Fluid fluid = entityPlayer.world.getFluid(entityPlayer.getChunkCoordinates());

        double d1;
        float f;
        float f2;
        if (entityPlayer.isInWater() && !grimPlayer.isFlying) {
            d1 = entityPlayer.locY();
            // 0.8F seems hardcoded in
            f = entityPlayer.isSprinting() ? 0.9F : 0.8F;
            float f1 = 0.02F;
            f2 = (float) EnchantmentManager.e(entityPlayer);
            if (f2 > 3.0F) {
                f2 = 3.0F;
            }

            if (!grimPlayer.lastOnGround) {
                f2 *= 0.5F;
            }

            if (f2 > 0.0F) {
                f += (0.54600006F - f) * f2 / 3.0F;
                f1 += (entityPlayer.dN() - f1) * f2 / 3.0F;
            }

            if (entityPlayer.hasEffect(MobEffects.DOLPHINS_GRACE)) {
                f = 0.96F;
            }

            NormalPrediction.guessBestMovement(f1, grimPlayer);
            grimPlayer.clientVelocity.add(moveRelative(f1, new Vector(grimPlayer.bestX, 0, grimPlayer.bestZ)));
            grimPlayer.predictedVelocity = grimPlayer.clientVelocity.clone();
            grimPlayer.clientVelocity = move(MoverType.SELF, grimPlayer.clientVelocity);

            grimPlayer.clientVelocity = grimPlayer.clientVelocity.multiply(new Vector(f, 0.8F, f));
            grimPlayer.clientVelocity = getFluidFallingAdjustedMovement(d, bl, grimPlayer.clientVelocity);

            if (grimPlayer.horizontalCollision && entityPlayer.e(grimPlayer.clientVelocity.getX(),
                    grimPlayer.clientVelocity.getY() + 0.6000000238418579D - grimPlayer.clientVelocity.getY() + d1,
                    grimPlayer.clientVelocity.getZ())) {
                grimPlayer.clientVelocity = grimPlayer.clientVelocity.multiply(
                        new Vector(grimPlayer.clientVelocity.getX(), 0.30000001192092896D, grimPlayer.clientVelocity.getZ()));
            }
        } else {
            if (entityPlayer.aQ() && entityPlayer.cT() && !entityPlayer.a(fluid.getType())) {
                d1 = grimPlayer.y;

                grimPlayer.clientVelocity = NormalPrediction.guessBestMovement(0.02F, grimPlayer);
                grimPlayer.clientVelocity.add(moveRelative(0.02F, new Vector(grimPlayer.bestX, 0, grimPlayer.bestZ)));
                grimPlayer.predictedVelocity = grimPlayer.clientVelocity.clone();
                grimPlayer.clientVelocity = move(MoverType.SELF, grimPlayer.clientVelocity);

                if (grimPlayer.fluidHeight.getOrDefault(FluidTag.LAVA, 0) <= entityPlayer.cx()) {
                    grimPlayer.clientVelocity = grimPlayer.clientVelocity.multiply(new Vector(0.5D, 0.800000011920929D, 0.5D));
                    grimPlayer.clientVelocity = getFluidFallingAdjustedMovement(d, bl, grimPlayer.clientVelocity);
                } else {
                    grimPlayer.clientVelocity.multiply(0.5D);
                }

                if (grimPlayer.bukkitPlayer.hasGravity()) {
                    grimPlayer.clientVelocity.add(new Vector(0.0D, -d / 4.0D, 0.0D));
                }

                if (grimPlayer.horizontalCollision && entityPlayer.e(grimPlayer.clientVelocity.getX(), grimPlayer.clientVelocity.getY() + 0.6000000238418579D - grimPlayer.y + d1, grimPlayer.clientVelocity.getZ())) {
                    grimPlayer.clientVelocity = new Vector(grimPlayer.clientVelocity.getX(), 0.30000001192092896D, grimPlayer.clientVelocity.getZ());
                }
                // TODO: Do inputs even matter while gliding?  What is there to predict?
            } else if (bukkitPlayer.isGliding()) {
                Vector lookVector = MovementVectorsCalc.getVectorForRotation(grimPlayer.xRot, grimPlayer.yRot);
                f = grimPlayer.yRot * 0.017453292F;
                double d2 = Math.sqrt(lookVector.getX() * lookVector.getX() + lookVector.getZ() * lookVector.getZ());
                double d3 = grimPlayer.clientVelocity.length();
                double d4 = lookVector.length();
                float f3 = MathHelper.cos(f);
                f3 = (float) ((double) f3 * (double) f3 * Math.min(1.0D, d4 / 0.4D));
                grimPlayer.clientVelocity = grimPlayer.clientVelocity.add(new Vector(0.0D, d * (-1.0D + (double) f3 * 0.75D), 0.0D));
                double d5;
                if (grimPlayer.clientVelocity.getY() < 0.0D && d2 > 0.0D) {
                    d5 = grimPlayer.clientVelocity.getY() * -0.1D * (double) f3;
                    grimPlayer.clientVelocity = grimPlayer.clientVelocity.add(new Vector(lookVector.getX() * d5 / d2, d5, lookVector.getZ() * d5 / d2));
                }

                if (f < 0.0F && d2 > 0.0D) {
                    d5 = d3 * (double) (-MathHelper.sin(f)) * 0.04D;
                    grimPlayer.clientVelocity = grimPlayer.clientVelocity.add(new Vector(-lookVector.getX() * d5 / d2, d5 * 3.2D, -lookVector.getZ() * d5 / d2));
                }

                if (d2 > 0.0D) {
                    grimPlayer.clientVelocity = grimPlayer.clientVelocity.add(new Vector((lookVector.getX() / d2 * d3 - grimPlayer.clientVelocity.getX()) * 0.1D, 0.0D, (lookVector.getZ() / d2 * d3 - grimPlayer.clientVelocity.getZ()) * 0.1D));
                }

                grimPlayer.clientVelocity = grimPlayer.clientVelocity.multiply(new Vector(0.9900000095367432D, 0.9800000190734863D, 0.9900000095367432D));
                grimPlayer.predictedVelocity = grimPlayer.clientVelocity.clone();
                grimPlayer.clientVelocity = move(MoverType.SELF, grimPlayer.clientVelocity);
                // IDK if there is a possible cheat for anti elytra damage
            } else {
                float blockFriction = BlockProperties.getBlockFriction(grimPlayer.bukkitPlayer);
                float f6 = grimPlayer.lastOnGround ? blockFriction * 0.91f : 0.91f;

                grimPlayer.clientVelocity = WithLadderPrediction.guessBestMovement(BlockProperties.getFrictionInfluencedSpeed(blockFriction, grimPlayer), grimPlayer);
                // This is a GIANT hack (while in dev)
                grimPlayer.predictedVelocity = grimPlayer.clientVelocity.clone();

                List<Vector> possibleMovements = new ArrayList<>();
                possibleMovements.add(grimPlayer.clientVelocity);

                // TODO: Which tick is accurate?
                if (grimPlayer.lastClimbing) {
                    possibleMovements.add(grimPlayer.clientVelocity.clone().setY(0.2));
                }

                grimPlayer.possibleMovementsWithAndWithoutLadders.clear();

                for (Vector vector : possibleMovements) {
                    vector = move(MoverType.SELF, vector);

                    // Okay, this seems to just be gravity stuff
                    double d9 = vector.getY();
                    if (bukkitPlayer.hasPotionEffect(PotionEffectType.LEVITATION)) {
                        d9 += (0.05 * (double) (bukkitPlayer.getPotionEffect(PotionEffectType.LEVITATION).getAmplifier() + 1) - grimPlayer.clientVelocity.getY()) * 0.2;
                        //this.fallDistance = 0.0f;
                    } else if (bukkitPlayer.getLocation().isChunkLoaded()) {
                        if (bukkitPlayer.hasGravity()) {
                            d9 -= d;
                        }
                    } else {
                        d9 = vector.getY() > 0.0 ? -0.1 : 0.0;
                    }

                    grimPlayer.possibleMovementsWithAndWithoutLadders.add(new Vector(vector.getX() * (double) f6, d9 * 0.9800000190734863, vector.getZ() * (double) f6));
                }
            }
        }
    }

    public Vector moveRelative(float f, Vector vec3) {
        return MovementVectorsCalc.getInputVector(vec3, f, bukkitPlayer.getLocation().getYaw());
    }

    // TODO: Do the best guess first for optimization

    // Entity line 527
    // TODO: Entity piston and entity shulker (want to) call this method too.
    // I want to transform this into the actual check
    // hmmm. what if I call this method with the player's actual velocity?
    // Sounds good :D
    public Vector move(MoverType moverType, Vector vec3) {
        // Something about noClip
        // Piston movement exemption
        // What is a motion multiplier?
        Vector clonedClientVelocity = Collisions.collide(Collisions.maybeBackOffFromEdge(vec3, moverType, grimPlayer), grimPlayer);

        grimPlayer.horizontalCollision = !Mth.equal(vec3.getX(), clonedClientVelocity.getX()) || !Mth.equal(vec3.getZ(), clonedClientVelocity.getZ());
        grimPlayer.verticalCollision = vec3.getY() != clonedClientVelocity.getY();

        if (vec3.getX() != clonedClientVelocity.getX()) {
            clonedClientVelocity.setX(0);
        }

        if (vec3.getZ() != clonedClientVelocity.getZ()) {
            clonedClientVelocity.setZ(0);
        }

        Block onBlock = BlockProperties.getOnBlock(grimPlayer);
        if (vec3.getY() != clonedClientVelocity.getY()) {
            if (onBlock.getType() == org.bukkit.Material.SLIME_BLOCK) {
                // TODO: Maybe lag compensate this (idk packet order)
                if (bukkitPlayer.isSneaking()) {
                    clonedClientVelocity.setY(0);
                } else {
                    if (clonedClientVelocity.getY() < 0.0) {
                        clonedClientVelocity.setY(-clonedClientVelocity.getY());
                    }
                }
            } else if (onBlock.getBlockData() instanceof Bed) {
                if (clonedClientVelocity.getY() < 0.0) {
                    clonedClientVelocity.setY(-grimPlayer.clientVelocity.getY() * 0.6600000262260437);
                }
            } else {
                clonedClientVelocity.setY(0);
            }
        }

        float f = BlockProperties.getBlockSpeedFactor(grimPlayer.bukkitPlayer);
        clonedClientVelocity.multiply(new Vector(f, 1.0, f));

        return clonedClientVelocity;
    }

    // LivingEntity line 1882
    // I have no clue what this does, but it really doesn't matter.  It works.
    public Vector getFluidFallingAdjustedMovement(double d, boolean bl, Vector vec3) {
        if (grimPlayer.bukkitPlayer.hasGravity() && !grimPlayer.bukkitPlayer.isSprinting()) {
            double d2 = bl && Math.abs(vec3.getY() - 0.005) >= 0.003 && Math.abs(vec3.getY() - d / 16.0) < 0.003 ? -0.003 : vec3.getY() - d / 16.0;
            return new Vector(vec3.getX(), d2, vec3.getZ());
        }
        return vec3;
    }

    public Vec3D getLookAngle() {
        return MovementVectorsCalc.calculateViewVector(grimPlayer.xRot, grimPlayer.yRot);
    }
}