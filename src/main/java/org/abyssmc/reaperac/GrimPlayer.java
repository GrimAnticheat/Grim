package org.abyssmc.reaperac;

import net.minecraft.server.v1_16_R3.EntityPlayer;
import net.minecraft.server.v1_16_R3.FluidType;
import net.minecraft.server.v1_16_R3.Tag;
import org.bukkit.Location;
import org.bukkit.craftbukkit.libs.it.unimi.dsi.fastutil.objects.Object2DoubleArrayMap;
import org.bukkit.craftbukkit.libs.it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class GrimPlayer {
    // TODO: Stop the player from setting abilities such as flying (Can they do this?)
    public Vector clientVelocity = new Vector();
    public Vector clientVelocityOnLadder = new Vector();
    public Vector clientVelocitySwimHop = new Vector();
    public Vector clientVelocityJumping = new Vector();

    public Vector predictedVelocity = new Vector();
    public Vector lastActualMovement = new Vector();
    public Vector actualMovement = new Vector();
    public Vector actualMovementCalculatedCollision = new Vector();
    public Vector stuckSpeedMultiplier = new Vector(1, 1, 1);
    public Player bukkitPlayer;
    public EntityPlayer entityPlayer;

    public double gravity;
    public float friction;

    // Set from packet
    public double x;
    public double y;
    public double z;
    public float xRot;
    public float yRot;
    public boolean onGround;
    public boolean isSneaking;
    public long movementEventMilliseconds;
    public long lastMovementEventMilliseconds;
    public long movementPacketMilliseconds;
    public long lastMovementPacketMilliseconds;

    // We determine this
    public boolean isActuallyOnGround;

    // We guess this
    public Vector theoreticalInput;
    public Vector possibleInput;
    public Vector bestOutput;

    // This should replace the previous block
    public Vector bestInputResult; // Use this for after trig is applied
    public Vector bestInputs; // Use this for debug, or preferably a party trick
    public Vector bestPreviousVelocity; // Use this for removing knockback from the list after using them

    // Set from base tick
    public Object2DoubleMap<Tag.e<FluidType>> fluidHeight = new Object2DoubleArrayMap<>(2);
    public boolean wasTouchingWater = false;
    public boolean wasEyeInWater = false;
    public Tag fluidOnEyes;

    // Placeholder, currently not used in any checks
    public double fallDistance = 0f;

    // Set after checks
    public double lastX;
    public double lastY;
    public double lastZ;
    public float lastXRot;
    public float lastYRot;
    public boolean lastOnGround;
    public boolean lastSneaking;
    public boolean horizontalCollision;
    public boolean verticalCollision;
    public boolean lastClimbing;

    public Location lastTickPosition;

    // Movement prediction stuff
    public Vector bestMovement = new Vector();

    // Possible inputs into the player's movement thing
    public List<Vector> possibleKnockback = new ArrayList<>();

    // Timer check data
    public long offset = 0L;
    public long lastMovementPacket = System.currentTimeMillis() - 50000000L;
    public boolean lastPacketIsReminder = false;

    public GrimPlayer(Player player) {
        this.bukkitPlayer = player;
        this.entityPlayer = ((CraftPlayer) player).getHandle();

        movementPacketMilliseconds = System.currentTimeMillis();
        lastMovementPacketMilliseconds = System.currentTimeMillis() - 100;

        Location loginLocation = player.getLocation();
        lastX = loginLocation.getX();
        lastY = loginLocation.getY();
        lastZ = loginLocation.getZ();
    }

    public List<Vector> getPossibleVelocities() {
        List<Vector> possibleMovements = getPossibleVelocitiesMinusKnockback();
        possibleMovements.addAll(possibleKnockback);

        return possibleMovements;
    }

    public List<Vector> getPossibleVelocitiesMinusKnockback() {
        List<Vector> possibleMovements = new ArrayList<>();
        possibleMovements.add(clientVelocity);

        if (clientVelocityJumping != null) {
            possibleMovements.add(clientVelocityJumping);
        }

        if (clientVelocityOnLadder != null) {
            possibleMovements.add(clientVelocityOnLadder);
        }

        if (clientVelocitySwimHop != null) {
            possibleMovements.add(clientVelocitySwimHop);
        }

        return possibleMovements;
    }

    public void baseTickAddVector(Vector vector) {
        clientVelocity.add(vector);

        if (clientVelocityJumping != null) {
            clientVelocityJumping.add(vector);
        }

        if (clientVelocityOnLadder != null)
            clientVelocityOnLadder.add(vector);

        if (clientVelocitySwimHop != null)
            clientVelocitySwimHop.add(vector);
    }

    public void baseTickSetX(double x) {
        clientVelocity.setX(x);

        if (clientVelocityJumping != null) {
            clientVelocityJumping.setX(x);
        }

        if (clientVelocityOnLadder != null)
            clientVelocityOnLadder.setX(x);

        if (clientVelocitySwimHop != null)
            clientVelocitySwimHop.setX(x);
    }

    public void baseTickSetY(double y) {
        clientVelocity.setY(y);

        if (clientVelocityJumping != null) {
            clientVelocityJumping.setY(y);
        }

        if (clientVelocityOnLadder != null)
            clientVelocityOnLadder.setY(y);

        if (clientVelocitySwimHop != null)
            clientVelocitySwimHop.setY(y);
    }

    public void baseTickSetZ(double z) {
        clientVelocity.setZ(z);

        if (clientVelocityJumping != null) {
            clientVelocityJumping.setZ(z);
        }

        if (clientVelocityOnLadder != null)
            clientVelocityOnLadder.setZ(z);

        if (clientVelocitySwimHop != null)
            clientVelocitySwimHop.setZ(z);
    }

    public void baseTickMultiplyY(double y) {
        clientVelocity.multiply(new Vector(1, y, 1));

        if (clientVelocityJumping != null) {
            clientVelocityJumping.multiply(new Vector(1, y, 1));
        }

        if (clientVelocityOnLadder != null)
            clientVelocityOnLadder.multiply(new Vector(1, y, 1));

        if (clientVelocitySwimHop != null)
            clientVelocitySwimHop.multiply(new Vector(1, y, 1));
    }


    public boolean isEyeInFluid(Tag tag) {
        return this.fluidOnEyes == tag;
    }
}