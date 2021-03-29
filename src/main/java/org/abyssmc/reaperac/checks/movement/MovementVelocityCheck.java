package org.abyssmc.reaperac.checks.movement;

import net.minecraft.server.v1_16_R3.EnchantmentManager;
import net.minecraft.server.v1_16_R3.EntityPlayer;
import net.minecraft.server.v1_16_R3.MathHelper;
import net.minecraft.server.v1_16_R3.MobEffects;
import org.abyssmc.reaperac.GrimPlayer;
import org.abyssmc.reaperac.ReaperAC;
import org.abyssmc.reaperac.checks.movement.predictions.PredictionEngineLava;
import org.abyssmc.reaperac.checks.movement.predictions.PredictionEngineNormal;
import org.abyssmc.reaperac.checks.movement.predictions.PredictionEngineWater;
import org.abyssmc.reaperac.events.anticheat.PlayerBaseTick;
import org.abyssmc.reaperac.utils.enums.FluidTag;
import org.abyssmc.reaperac.utils.enums.MoverType;
import org.abyssmc.reaperac.utils.math.MovementVectorsCalc;
import org.abyssmc.reaperac.utils.math.Mth;
import org.abyssmc.reaperac.utils.nmsImplementations.BlockProperties;
import org.abyssmc.reaperac.utils.nmsImplementations.Collisions;
import org.abyssmc.reaperac.utils.nmsImplementations.FluidFallingAdjustedMovement;
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
        //
        // This would error when the player has mob collision
        // I should probably separate mob and block collision
        grimPlayer.actualMovementCalculatedCollision = Collisions.collide(Collisions.maybeBackOffFromEdge(grimPlayer.actualMovement.clone(), MoverType.SELF, grimPlayer), grimPlayer);

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
        /*if (grimPlayer.isActuallyOnGround != grimPlayer.lastOnGround) {
            Bukkit.broadcastMessage("Failed on ground, client believes: " + grimPlayer.onGround);
        }*/

        if (grimPlayer.predictedVelocity.distanceSquared(grimPlayer.actualMovement) > new Vector(0.03, 0.03, 0.03).lengthSquared()) {
            //Bukkit.broadcastMessage(ChatColor.RED + "FAILED MOVEMENT CHECK");
        }

        grimPlayer.lastActualMovement = grimPlayer.actualMovement;
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

        if (grimPlayer.bukkitPlayer.isFlying() && grimPlayer.bukkitPlayer.getVehicle() == null) {
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

        EntityPlayer entityPlayer = grimPlayer.entityPlayer;

        double lastY;
        float swimFriction;
        float f2;

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

                if (grimPlayer.bukkitPlayer.hasGravity()) {
                    grimPlayer.clientVelocity.add(new Vector(0.0D, -playerGravity / 4.0D, 0.0D));
                }

                if (grimPlayer.horizontalCollision && entityPlayer.e(grimPlayer.clientVelocity.getX(), grimPlayer.clientVelocity.getY() + 0.6000000238418579D - grimPlayer.y + lastY, grimPlayer.clientVelocity.getZ())) {
                    grimPlayer.clientVelocity = new Vector(grimPlayer.clientVelocity.getX(), 0.30000001192092896D, grimPlayer.clientVelocity.getZ());
                }

            } else if (bukkitPlayer.isGliding()) {
                Vector lookVector = MovementVectorsCalc.getVectorForRotation(grimPlayer.yRot, grimPlayer.xRot);
                Vector clientVelocity = grimPlayer.clientVelocity.clone();
                Vector elytraVelocity = grimPlayer.clientVelocity.clone();

                double d2 = Math.sqrt(lookVector.getX() * lookVector.getX() + lookVector.getZ() * lookVector.getZ());

                if (d2 > 0.0D) {
                    clientVelocity = getElytraMovement(clientVelocity);
                }

                // Under 11 means the firework might have ended (there's a bit of randomness)
                if (grimPlayer.fireworkElytraDuration <= 11) {
                    grimPlayer.currentlyUsingFirework = false;
                }

                if (grimPlayer.fireworkElytraDuration > 0) {
                    elytraVelocity = grimPlayer.clientVelocity.clone().add(new Vector(lookVector.getX() * 0.1 + (lookVector.getX() * 1.5 - grimPlayer.clientVelocity.getX()) * 0.5, lookVector.getY() * 0.1 + (lookVector.getY() * 1.5 - grimPlayer.clientVelocity.getY()) * 0.5, (lookVector.getZ() * 0.1 + (lookVector.getZ() * 1.5 - grimPlayer.clientVelocity.getZ()) * 0.5)).multiply(new Vector(0.99F, 0.98F, 0.99F)));

                    elytraVelocity = getElytraMovement(elytraVelocity);
                }

                Bukkit.broadcastMessage("Distance to elytra " + elytraVelocity.distanceSquared(grimPlayer.actualMovement));
                Bukkit.broadcastMessage("Distance to client " + clientVelocity.distanceSquared(grimPlayer.actualMovement));

                if (grimPlayer.currentlyUsingFirework || grimPlayer.fireworkElytraDuration > 0 && elytraVelocity.distanceSquared(grimPlayer.actualMovement) < clientVelocity.distanceSquared(grimPlayer.actualMovement)) {
                    grimPlayer.clientVelocity = elytraVelocity;
                    grimPlayer.currentlyUsingFirework = true;
                    Bukkit.broadcastMessage("Used a firework");
                    grimPlayer.fireworkElytraDuration--;
                } else {
                    grimPlayer.clientVelocity = clientVelocity;
                    Bukkit.broadcastMessage("No");
                }

                grimPlayer.clientVelocity.multiply(new Vector(0.99F, 0.98F, 0.99F));
                grimPlayer.predictedVelocity = grimPlayer.clientVelocity.clone();
                grimPlayer.clientVelocity = move(grimPlayer, MoverType.SELF, grimPlayer.clientVelocity);

            } else {
                float blockFriction = BlockProperties.getBlockFriction(grimPlayer.bukkitPlayer);
                float f6 = grimPlayer.lastOnGround ? blockFriction * 0.91f : 0.91f;
                grimPlayer.gravity = playerGravity;
                grimPlayer.friction = f6;

                new PredictionEngineNormal().guessBestMovement(BlockProperties.getFrictionInfluencedSpeed(blockFriction, grimPlayer), grimPlayer);
            }
        }
    }

    public Vector getElytraMovement(Vector vector) {
        Vector lookVector = MovementVectorsCalc.getVectorForRotation(grimPlayer.yRot, grimPlayer.xRot);

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

        vector.add(new Vector((lookVector.getX() / d2 * d3 - vector.getX()) * 0.1D, 0.0D, (lookVector.getZ() / d2 * d3 - vector.getZ()) * 0.1D));

        return vector;
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

        if (vec3.getX() != clonedClientVelocity.getX()) {
            clonedClientVelocity.setX(0);
        }

        if (vec3.getZ() != clonedClientVelocity.getZ()) {
            clonedClientVelocity.setZ(0);
        }

        Location getBlockLocation;
        // Stop "blinking" to slime blocks
        // 0.5 blocks is a huge buffer but it nerfs the cheats "enough"
        // Use the player's new location for better accuracy
        if (grimPlayer.predictedVelocity.distance(grimPlayer.actualMovement) < 0.5) {
            getBlockLocation = new Location(grimPlayer.bukkitPlayer.getWorld(), grimPlayer.x, grimPlayer.y - 0.2F, grimPlayer.z);
        } else {
            getBlockLocation = grimPlayer.bukkitPlayer.getLocation().add(grimPlayer.clientVelocity).subtract(0, 0.2, 0);
        }

        Block onBlock = BlockProperties.getOnBlock(getBlockLocation);

        if (vec3.getY() != clonedClientVelocity.getY()) {
            if (onBlock.getType() == org.bukkit.Material.SLIME_BLOCK) {
                // TODO: Maybe lag compensate this (idk packet order)
                if (grimPlayer.bukkitPlayer.isSneaking()) {
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

        float f = BlockProperties.getBlockSpeedFactor(grimPlayer.bukkitPlayer);
        clonedClientVelocity.multiply(new Vector(f, 1.0, f));

        return clonedClientVelocity;
    }
}