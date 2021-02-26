package org.abyssmc.reaperac.checks.movement;

import net.minecraft.server.v1_16_R3.*;
import org.abyssmc.reaperac.GrimPlayer;
import org.abyssmc.reaperac.ReaperAC;
import org.abyssmc.reaperac.utils.enums.MoverType;
import org.abyssmc.reaperac.utils.math.Mth;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Fence;
import org.bukkit.block.data.type.Gate;
import org.bukkit.block.data.type.Wall;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_16_R3.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.stream.Stream;

public class MovementVelocityCheck implements BaseMovementCheck {
    private static final double jumpingEpsilon = 0.01d;
    private static final double maxUpStep = 0.6f;
    GrimPlayer grimPlayer;
    Player player;

    double x;
    double y;
    double z;
    float xRot;
    float yRot;
    boolean onGround;

    public MovementVelocityCheck(GrimPlayer player, double x, double y, double z, float xRot, float yRot, boolean onGround) {
        this.grimPlayer = player;
        this.player = player.bukkitPlayer;
        this.x = x;
        this.y = y;
        this.z = z;
        this.xRot = xRot;
        this.yRot = yRot;
        this.onGround = onGround;

        player.actualMovement = new Vector(x - player.lastX, y - player.lastY, z - player.lastZ);

        // We can't do everything fully async because getting entities - https://pastebin.com/s0XhgCvV
        Bukkit.getScheduler().runTask(ReaperAC.plugin, () -> {
            livingEntityAIStep();

            Bukkit.broadcastMessage("Predicted: " + ChatColor.BLUE + player.predictedVelocity.getX() + " " + ChatColor.AQUA + player.predictedVelocity.getY() + " " + ChatColor.GREEN + player.predictedVelocity.getZ());
            Bukkit.broadcastMessage("Actually:  " + ChatColor.BLUE + player.actualMovement.getX() + " " + ChatColor.AQUA + player.actualMovement.getY() + " " + ChatColor.GREEN + player.actualMovement.getZ());

            player.lastActualMovement = player.actualMovement;

            player.lastX = x;
            player.lastY = y;
            player.lastZ = z;
            player.lastXRot = xRot;
            player.lastYRot = yRot;
            player.lastOnGround = onGround;
        });
    }

    public void livingEntityAIStep() {
        // not sure if this is correct
        // Living Entity line 2153 (fuck, must have switched mappings)
        //clientVelocity.multiply(0.98f);

        // Living Entity line 2153
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
        //float sidewaysSpeed = 0f;
        //float forwardsSpeed = 1f;

        // random stuff about jumping in liquids
        // TODO: Jumping in liquids

        if (Math.abs(grimPlayer.actualMovement.getY() - grimPlayer.lastActualMovement.getY() - getJumpPower()) < jumpingEpsilon) {
            jumpFromGround();
        }

        // Living Entity line 2202
        //sidewaysSpeed *= 0.98f;
        //forwardsSpeed *= 0.98f;

        //Vector inputVector = new Vector(sidewaysSpeed, 0, forwardsSpeed);

        // Living entity line 2206
        //livingEntityTravel(inputVector);
        livingEntityTravel();


        //clientVelocity.multiply(0.98f);
    }

    public float getJumpPower() {
        return 0.42f * getPlayerJumpFactor();
    }

    private void jumpFromGround() {
        float f = getJumpPower();

        if (player.hasPotionEffect(PotionEffectType.JUMP)) {
            f += 0.1f * (float) (player.getPotionEffect(PotionEffectType.JUMP).getAmplifier() + 1);
        }

        grimPlayer.clientVelocity.setY(f);

        // TODO: Use the stuff from the sprinting packet
        if (player.isSprinting()) {
            // TODO: Do we use new or old rotation?  It should be new...
            float f2 = xRot * 0.017453292f;
            grimPlayer.clientVelocity.add(new Vector(-Mth.sin(f2) * 0.2f, 0.0, Mth.cos(f2) * 0.2f));
        }
    }

    // LivingEntity line 1741
    public void livingEntityTravel() {
        double d = 0.08;

        float blockFriction = getBlockFriction();
        float f6 = onGround ? blockFriction * 0.91f : 0.91f;
        // TODO: Figure this shit out!
        Vector vec37 = handleRelativeFrictionAndCalculateMovement(blockFriction);

        // Okay, this seems to just be gravity stuff
        double d9 = grimPlayer.clientVelocity.getY();
        if (player.hasPotionEffect(PotionEffectType.LEVITATION)) {
            d9 += (0.05 * (double) (player.getPotionEffect(PotionEffectType.LEVITATION).getAmplifier() + 1) - vec37.getY()) * 0.2;
            //this.fallDistance = 0.0f;
        } else if (player.getLocation().isChunkLoaded()) {
            if (player.hasGravity()) {
                d9 -= d;
            }
        } else {
            d9 = grimPlayer.clientVelocity.getY() > 0.0 ? -0.1 : 0.0;
        }

        grimPlayer.predictedVelocity = grimPlayer.clientVelocity;

        // TODO: This might not be correct
        grimPlayer.clientVelocity = new Vector(vec37.getX() * (double) f6, d9 * 0.9800000190734863, vec37.getZ() * (double) f6);
    }

    private float getPlayerJumpFactor() {
        float f = ((CraftBlockData) player.getWorld().getBlockAt
                (player.getLocation().getBlockX(), player.getLocation().getBlockY(), player.getLocation().getBlockZ())
                .getBlockData()).getState().getBlock().getJumpFactor();
        float f2 = ((CraftBlockData) player.getWorld().getBlockAt
                (player.getLocation().getBlockX(), (int) (player.getBoundingBox().getMinY() - 0.5000001),
                        player.getLocation().getBlockZ()).getBlockData()).getState().getBlock().getJumpFactor();

        return (double) f == 1.0 ? f2 : f;
    }

    // TODO: this code is shit
    // Seems to work.
    public float getBlockFriction() {
        return ((CraftBlockData) player.getWorld().getBlockAt
                (player.getLocation().getBlockX(), (int) (player.getBoundingBox().getMinY() - 0.5000001),
                        player.getLocation().getBlockZ())
                .getBlockData()).getState().getBlock().getFrictionFactor();
    }

    // Line 1871 LivingEntity
    public Vector handleRelativeFrictionAndCalculateMovement(float f) {
        f = this.getFrictionInfluencedSpeed(f);

        /*double movementAngle = Math.atan2(wantedMovement.getX(), wantedMovement.getZ());
        double lookAngle = player.getLocation().getYaw();
        double relativeAngle = (movementAngle - lookAngle + 360 ) % 360;
        int angle = (int) (relativeAngle / 4);

        Vector movementOne = getInputVector(new Vector(0.98,0,0.98), f, player.getLocation().getYaw());
        Vector movementTwo = getInputVector(new Vector(0.98,0,0.98), f, player.getLocation().getYaw());

        switch (angle) {
            case 0:
                if (wantedMovement)
            case 1:
                //
            case 2:
                //
            case 3:
                //
        }*/

        double bestMovementGuess = Integer.MAX_VALUE;
        double bestMovementX = 0;
        double bestMovementZ = 1;

        Vector yIgnoredVector = grimPlayer.actualMovement.clone().subtract(grimPlayer.clientVelocity);
        yIgnoredVector.setY(0);

        // Fuck optimization before things work... let's see if the theory is good

        for (int movementX = -1; movementX <= 1; movementX++) {
            for (int movementZ = -1; movementZ <= 1; movementZ++) {
                double movementXWithShifting = movementX;
                double movementZWithShifting = movementZ;

                if (player.isSneaking()) {
                    movementXWithShifting *= 0.3;
                    movementZWithShifting *= 0.3;
                }

                Vector clonedClientVelocity = grimPlayer.clientVelocity.clone();
                Vector movementInput = getInputVector(new Vector(movementXWithShifting * 0.98, 0, movementZWithShifting * 0.98), f, player.getLocation().getYaw());
                clonedClientVelocity.add(movementInput);
                clonedClientVelocity = move(MoverType.SELF, new Vec3D(clonedClientVelocity.getX(), 0, clonedClientVelocity.getZ()));

                double closeness = grimPlayer.actualMovement.clone().subtract(clonedClientVelocity).lengthSquared();

                if (closeness < bestMovementGuess) {
                    bestMovementGuess = closeness;
                    bestMovementX = movementXWithShifting;
                    bestMovementZ = movementZWithShifting;
                }
            }
        }

        Bukkit.broadcastMessage("Guessed inputs: " + bestMovementZ + " " + bestMovementX);

        Vector movementInput = getInputVector(new Vector(bestMovementX * 0.98, 0, bestMovementZ * 0.98), f, player.getLocation().getYaw());
        grimPlayer.clientVelocity = grimPlayer.clientVelocity.add(movementInput);

        grimPlayer.clientVelocity = move(MoverType.SELF, getClientVelocityAsVec3D());

        return grimPlayer.clientVelocity;
    }

    // Verified.  This is correct.
    private float getFrictionInfluencedSpeed(float f) {
        if (player.isOnGround()) {
            // Required because getting player walk speed doesn't talk into account sprinting
            //if (player.isSprinting()) {
            //    g *= 1.30000001192092896;
            //}

            return (float) (player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getValue() * (0.21600002f / (f * f * f)));
        }
        return player.getFlySpeed();
    }

    // Entity line 1046
    private static Vector getInputVector(Vector vec3, float f, float f2) {
        // idk why this is needed, but it was fucking up input for other stuff
        double d = vec3.lengthSquared();
        if (d < 1.0E-7) {
            return new Vector();
        }
        Vector vec32 = (d > 1.0 ? vec3.normalize() : vec3).multiply(f);
        float f3 = Mth.sin(f2 * 0.017453292f);
        float f4 = Mth.cos(f2 * 0.017453292f);
        return new Vector(vec32.getX() * (double) f4 - vec32.getZ() * (double) f3,
                vec32.getY(), vec32.getZ() * (double) f4 + vec32.getX() * (double) f3);
    }

    // Entity line 527
    public Vector move(MoverType moverType, Vec3D vec3) {
        Vec3D vec32;
        Vector clonedClientVelocity = grimPlayer.clientVelocity.clone();

        // Something about noClip
        // Piston movement exemption
        // What is a motion multiplier?
        // TODO: Motion multiplier

        // We might lose 0.0000001 precision here at worse for no if statement
        clonedClientVelocity = this.collide(this.maybeBackOffFromEdge(vec3, moverType));
        //this.setBoundingBox(this.getBoundingBox().move(vec32));
        //this.setLocationFromBoundingbox();

        // TODO: Block collision code
        Block onBlock = getOnBlock();
        // something about resetting fall state - not sure if server has functioning fall distance tracker
        // I'm being hopeful, of course the server's fall distance tracker is broken
        // TODO: Fall damage stuff
        // I need block collision code to accurately do y distance


        float f = getBlockSpeedFactor();
        clonedClientVelocity.multiply(new Vector(f, 1.0, f));

        return clonedClientVelocity;
    }

    public Vec3D getClientVelocityAsVec3D() {
        return new Vec3D(grimPlayer.clientVelocity.getX(), grimPlayer.clientVelocity.getY(), grimPlayer.clientVelocity.getZ());
    }

    // Entity line 686
    private Vector collide(Vec3D vec3) {
        boolean bl;
        AxisAlignedBB aABB = grimPlayer.entityPlayer.getBoundingBox();
        VoxelShapeCollision collisionContext = VoxelShapeCollision.a(grimPlayer.entityPlayer);
        VoxelShape voxelShape = grimPlayer.entityPlayer.getWorld().getWorldBorder().c();
        Stream<VoxelShape> stream = VoxelShapes.c(voxelShape, VoxelShapes.a(aABB.shrink(1.0E-7)), OperatorBoolean.AND) ? Stream.empty() : Stream.of(voxelShape);
        Stream<VoxelShape> stream2 = grimPlayer.entityPlayer.getWorld().c(grimPlayer.entityPlayer, aABB.b(vec3), entity -> true);
        StreamAccumulator<VoxelShape> rewindableStream = new StreamAccumulator<>(Stream.concat(stream2, stream));
        Vec3D vec32 = vec3.g() == 0.0 ? vec3 : Entity.a(grimPlayer.entityPlayer, vec3, aABB, grimPlayer.entityPlayer.getWorld(), collisionContext, rewindableStream);
        boolean bl2 = vec3.x != vec32.x;
        boolean bl3 = vec3.y != vec32.y;
        boolean bl4 = vec3.z != vec32.z;
        boolean bl5 = bl = player.isOnGround() || bl3 && vec3.y < 0.0;
        if (bl && (bl2 || bl4)) {
            Vec3D vec33;
            Vec3D vec34 = Entity.a(grimPlayer.entityPlayer, new Vec3D(vec3.x, maxUpStep, vec3.z), aABB, grimPlayer.entityPlayer.getWorld(), collisionContext, rewindableStream);
            Vec3D vec35 = Entity.a(grimPlayer.entityPlayer, new Vec3D(0.0, maxUpStep, 0.0), aABB.b(vec3.x, 0.0, vec3.z), grimPlayer.entityPlayer.getWorld(), collisionContext, rewindableStream);
            if (vec35.y < maxUpStep && Entity.c(vec33 = Entity.a(grimPlayer.entityPlayer, new Vec3D(vec3.x, 0.0, vec3.z), AxisAlignedBB.a(vec35), grimPlayer.entityPlayer.getWorld(), collisionContext, rewindableStream).e(vec35)) > Entity.c(vec34)) {
                vec34 = vec33;
            }
            if (Entity.c(vec34) > Entity.c(vec32)) {
                vec34.e(Entity.a(grimPlayer.entityPlayer, new Vec3D(0.0, -vec34.y + vec3.y, 0.0), aABB.c(vec34), grimPlayer.entityPlayer.getWorld(), collisionContext, rewindableStream));
                return new Vector(vec34.x, vec34.y, vec34.z);
            }
        }
        return new Vector(vec32.x, vec32.y, vec32.z);
    }

    // MCP mappings PlayerEntity 959
    // Mojang mappings 936
    protected Vec3D maybeBackOffFromEdge(Vec3D vec3, MoverType moverType) {
        if (!player.isFlying() && (moverType == MoverType.SELF || moverType == MoverType.PLAYER) && player.isSneaking() && isAboveGround()) {
            double d = vec3.getX();
            double d2 = vec3.getZ();
            while (d != 0.0 && ((CraftWorld) player.getWorld()).getHandle().getCubes(((CraftPlayer) player).getHandle(),
                    ((CraftPlayer) player).getHandle().getBoundingBox().d(d, -maxUpStep, 0.0))) {
                if (d < 0.05 && d >= -0.05) {
                    d = 0.0;
                    continue;
                }
                if (d > 0.0) {
                    d -= 0.05;
                    continue;
                }
                d += 0.05;
            }
            while (d2 != 0.0 && ((CraftWorld) player.getWorld()).getHandle().getCubes(((CraftPlayer) player).getHandle(),
                    ((CraftPlayer) player).getHandle().getBoundingBox().d(0.0, -maxUpStep, d2))) {
                if (d2 < 0.05 && d2 >= -0.05) {
                    d2 = 0.0;
                    continue;
                }
                if (d2 > 0.0) {
                    d2 -= 0.05;
                    continue;
                }
                d2 += 0.05;
            }
            while (d != 0.0 && d2 != 0.0 && ((CraftWorld) player.getWorld()).getHandle().getCubes(((CraftPlayer) player).getHandle(),
                    ((CraftPlayer) player).getHandle().getBoundingBox().d(d, -maxUpStep, d2))) {
                d = d < 0.05 && d >= -0.05 ? 0.0 : (d > 0.0 ? (d -= 0.05) : (d += 0.05));
                if (d2 < 0.05 && d2 >= -0.05) {
                    d2 = 0.0;
                    continue;
                }
                if (d2 > 0.0) {
                    d2 -= 0.05;
                    continue;
                }
                d2 += 0.05;
            }
            vec3 = new Vec3D(d, vec3.getY(), d2);
        }
        return vec3;
    }

    // Entity line 617
    // Heavily simplified (wtf was that original code mojang)
    private Block getOnBlock() {
        Block block1 = player.getWorld().getBlockAt(player.getLocation().getBlockX(), (int) (player.getLocation().getX() - 0.2F), player.getLocation().getBlockZ());
        Block block2 = player.getWorld().getBlockAt(player.getLocation().getBlockX(), (int) (player.getLocation().getX() - 1.2F), player.getLocation().getBlockZ());

        if (block2.getType().isAir()) {
            if (block2 instanceof Fence || block2 instanceof Wall || block2 instanceof Gate) {
                return block2;
            }
        }

        return block1;
    }

    // Entity line 637
    // Seems fine to me.  Haven't found issues here
    public float getBlockSpeedFactor() {
        net.minecraft.server.v1_16_R3.Block block = ((CraftBlockData) player.getWorld().getBlockAt
                (player.getLocation().getBlockX(), player.getLocation().getBlockY(),
                        player.getLocation().getBlockZ())
                .getBlockData()).getState().getBlock();
        float f = block.getSpeedFactor();
        if (block == net.minecraft.server.v1_16_R3.Blocks.WATER || block == net.minecraft.server.v1_16_R3.Blocks.BUBBLE_COLUMN) {
            return f;
        }
        return (double) f == 1.0 ? ((CraftBlockData) player.getWorld().getBlockAt
                (player.getLocation().getBlockX(), (int) (player.getBoundingBox().getMinY() - 0.5000001),
                        player.getLocation().getBlockZ())
                .getBlockData()).getState().getBlock().getSpeedFactor() : f;
    }

    // What the fuck is this?
    private boolean isAboveGround() {
        return player.isOnGround() || player.getFallDistance() < maxUpStep && !
                ((CraftWorld) player.getWorld()).getHandle().getCubes(((CraftPlayer) player).getHandle(), ((CraftPlayer) player).getHandle().getBoundingBox().d(0.0, player.getFallDistance() - maxUpStep, 0.0));
    }
}
