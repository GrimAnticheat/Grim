package org.abyssmc.reaperac.players;

import net.minecraft.server.v1_16_R3.Block;
import net.minecraft.server.v1_16_R3.Blocks;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.v1_16_R3.block.data.CraftBlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

public class GrimPlayer implements Listener {
    public boolean isFlying;
    public boolean allowFlying;
    public boolean instantBreak;
    public Vector clientVelocity = new Vector();
    public Vector predictedVelocity;
    public Vector lastMovement = new Vector();
    public double x;
    public double y;
    public double z;
    Player player;

    public GrimPlayer(Player player) {
        this.player = player;
    }

    // Entity line 1046
    private static Vector getInputVector(Vector vec3, float f, float f2) {
        // idk why this is needed, but it was fucking up input for other stuff
        double d = vec3.lengthSquared();
        if (d < 1.0E-7) {
            return new Vector();
        }
        Vector vec32 = (d > 1.0 ? vec3.normalize() : vec3).multiply(f);
        float f3 = (float) Math.sin(f2 * 0.017453292f);
        float f4 = (float) Math.cos(f2 * 0.017453292f);
        return new Vector(vec32.getX() * (double) f4 - vec32.getZ() * (double) f3,
                vec32.getY(), vec32.getZ() * (double) f4 + vec32.getX() * (double) f3);
    }

    @EventHandler
    public void onPlayerMoveEvent(PlayerMoveEvent event) {
        x = event.getFrom().getX();
        y = event.getFrom().getY();
        z = event.getFrom().getZ();


        // TODO: Trusting the client on ground is a bad idea unless we check is somewhere else
        Location actualMovement = event.getTo().clone().subtract(event.getFrom());

        livingEntityMove();

        Bukkit.broadcastMessage("Predicted: " + predictedVelocity.getX() + " " + predictedVelocity.getZ());
        Bukkit.broadcastMessage("Actually:  " + actualMovement.getX() + " " + actualMovement.getZ());
    }

    public void livingEntityMove() {
        // not sure if this is correct
        // Living Entity line 2153 (fuck, must have switched mappings)
        //clientVelocity.multiply(0.98f);

        // Living Entity line 2153
        if (Math.abs(clientVelocity.getX()) < 0.003D) {
            clientVelocity.setX(0D);
        }

        if (Math.abs(clientVelocity.getY()) < 0.003D) {
            clientVelocity.setY(0D);
        }

        if (Math.abs(clientVelocity.getZ()) < 0.003D) {
            clientVelocity.setZ(0D);
        }

        // Now it gets input
        // Now it does jumping and fluid movement

        // Living Entity line 2180
        float sidewaysSpeed = 0f;
        float forwardsSpeed = 1f;

        // Living Entity line 2202
        sidewaysSpeed *= 0.98f;
        forwardsSpeed *= 0.98f;

        Vector inputVector = new Vector(sidewaysSpeed, 0, forwardsSpeed);

        // Living entity line 2206
        livingEntityTravel(inputVector);


        //clientVelocity.multiply(0.98f);
    }

    // LivingEntity line 1741
    public void livingEntityTravel(Vector vec3) {
        float blockFriction = getBlockFriction();
        float f6 = player.isOnGround() ? blockFriction * 0.91f : 0.91f;
        // TODO: Figure this shit out!
        Vector vec37 = handleRelativeFrictionAndCalculateMovement(vec3, blockFriction);
        double d9 = clientVelocity.getY();
        /*if (this.hasEffect(MobEffects.LEVITATION)) {
            d9 += (0.05 * (double)(this.getEffect(MobEffects.LEVITATION).getAmplifier() + 1) - vec37.y) * 0.2;
            this.fallDistance = 0.0f;
        } else if (!this.level.isClientSide || this.level.hasChunkAt(blockPos)) {
            if (!this.isNoGravity()) {
                d9 -= d;
            }
        } else {*/
        d9 = this.getY() > 0.0 ? -0.1 : 0.0;
        //}

        predictedVelocity = clientVelocity;

        // TODO: This might not be correct
        clientVelocity = new Vector(vec37.getX() * (double) f6, d9 * 0.9800000190734863, vec37.getZ() * (double) f6);
    }


    // Line 1871 LivingEntity
    public Vector handleRelativeFrictionAndCalculateMovement(Vector vec3, float f) {
        this.moveRelative(this.getFrictionInfluencedSpeed(f), vec3);
        //this.setDeltaMovement(this.handleOnClimbable(this.getDeltaMovement()));
        /*if ((this.horizontalCollision || this.jumping) && this.onClimbable()) {
            vec32 = new Vec3(vec32.x, 0.2, vec32.z);
        }*/
        move();
        return clientVelocity;
    }

    public void move() {
        // TODO: Block collision code
        float f = getBlockSpeedFactor();
        clientVelocity.multiply(new Vector(f, 1.0, f));
    }

    // Entity line 1041
    public void moveRelative(float f, Vector vec3) {
        // TODO: This is where you try to figure out input
        Vector movementInput = getInputVector(vec3, f, player.getLocation().getYaw());
        clientVelocity = clientVelocity.add(movementInput);
    }

    // TODO: this code is shit
    // Seems to work.
    public float getBlockFriction() {
        return ((CraftBlockData) player.getWorld().getBlockAt
                (player.getLocation().getBlockX(), (int) (player.getBoundingBox().getMinY() - 0.5000001),
                        player.getLocation().getBlockZ())
                .getBlockData()).getState().getBlock().getFrictionFactor();
    }

    // Entity line 637
    // Seems fine to me.  Haven't found issues here
    public float getBlockSpeedFactor() {
        Block block = ((CraftBlockData) player.getWorld().getBlockAt
                (player.getLocation().getBlockX(), player.getLocation().getBlockY(),
                        player.getLocation().getBlockZ())
                .getBlockData()).getState().getBlock();
        float f = block.getSpeedFactor();
        if (block == Blocks.WATER || block == Blocks.BUBBLE_COLUMN) {
            return f;
        }
        return (double) f == 1.0 ? ((CraftBlockData) player.getWorld().getBlockAt
                (player.getLocation().getBlockX(), (int) (player.getBoundingBox().getMinY() - 0.5000001),
                        player.getLocation().getBlockZ())
                .getBlockData()).getState().getBlock().getSpeedFactor() : f;
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

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }
}