package ac.grim.grimac;

import ac.grim.grimac.utils.data.BoatData;
import ac.grim.grimac.utils.data.FireworkData;
import io.github.retrooper.packetevents.PacketEvents;
import net.minecraft.server.v1_16_R3.AxisAlignedBB;
import net.minecraft.server.v1_16_R3.EntityPlayer;
import net.minecraft.server.v1_16_R3.FluidType;
import net.minecraft.server.v1_16_R3.Tag;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.libs.it.unimi.dsi.fastutil.objects.Object2DoubleArrayMap;
import org.bukkit.craftbukkit.libs.it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class GrimPlayer {
    public final UUID playerUUID;
    // This is the most essential value and controls the threading
    public AtomicInteger tasksNotFinished = new AtomicInteger(0);
    public Player bukkitPlayer;
    public EntityPlayer entityPlayer;
    public int entityID;
    public short clientVersion;

    public AtomicInteger taskNumber = new AtomicInteger(0);

    public Vector clientVelocity = new Vector();
    public Vector clientVelocityOnLadder = new Vector();
    public Vector clientVelocitySwimHop = new Vector();
    public Vector clientVelocityFireworkBoost = new Vector();

    public Vector predictedVelocity = new Vector();
    public Vector actualMovement = new Vector();
    public Vector stuckSpeedMultiplier = new Vector(1, 1, 1);
    public Vector blockSpeedMultiplier = new Vector(1, 1, 1);
    public Vector lastStuckSpeedMultiplier = new Vector(1, 1, 1);

    public double gravity;
    public float friction;
    public float speed;

    // Set from packet
    public double x;
    public double y;
    public double z;
    public float xRot;
    public float yRot;
    public boolean onGround;
    // Packet sprinting isn't lag compensated, regular one is
    public boolean isPacketSneaking;
    public boolean isPacketSprinting;

    // Set from the time that the movement packet was received, to be thread safe
    public boolean isSneaking;
    public boolean wasSneaking;
    public boolean isSprinting;
    public boolean isFlying;
    public boolean specialFlying;
    public boolean isSwimming;
    public boolean isClimbing;
    public boolean isFallFlying;
    public double fallDistance;
    public AxisAlignedBB boundingBox;
    public World playerWorld;

    public double movementSpeed;
    public float jumpAmplifier;
    public float levitationAmplifier;
    public float flySpeed;

    public boolean inVehicle;
    public Entity playerVehicle;
    public float packetVehicleHorizontal;
    public float packetVehicleForward;
    public float vehicleHorizontal;
    public float vehicleForward;
    public BoatData boatData = new BoatData();

    // We determine this
    public boolean isActuallyOnGround;

    // Set from base tick
    public Object2DoubleMap<Tag.e<FluidType>> fluidHeight = new Object2DoubleArrayMap<>(2);
    public boolean wasTouchingWater = false;
    public boolean wasEyeInWater = false;
    public Tag fluidOnEyes;

    // Handled by entity spawn event, removed when firework dies
    public HashMap<Integer, FireworkData> fireworks = new HashMap<>();

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
    public boolean couldSkipTick = false;

    // Possible inputs into the player's movement thing
    public List<Vector> possibleKnockback = new ArrayList<>();

    // Keep track of basetick stuff
    public Vector baseTickSet;
    public Vector baseTickAddition;
    public short lastTransactionReceived = 0;
    public short movementTransaction = Short.MIN_VALUE;

    // Determining player ping
    ConcurrentHashMap<Short, Long> transactionsSent = new ConcurrentHashMap<>();

    public GrimPlayer(Player player) {
        this.bukkitPlayer = player;
        this.entityPlayer = ((CraftPlayer) player).getHandle();
        this.playerUUID = player.getUniqueId();
        this.entityID = player.getEntityId();
        this.clientVersion = PacketEvents.get().getPlayerUtils().getClientVersion(player).getProtocolVersion();

        Location loginLocation = player.getLocation();
        lastX = loginLocation.getX();
        lastY = loginLocation.getY();
        lastZ = loginLocation.getZ();
    }

    public Set<Vector> getPossibleVelocities() {
        Set<Vector> possibleMovements = getPossibleVelocitiesMinusKnockback();
        possibleMovements.addAll(possibleKnockback);

        return possibleMovements;
    }

    public Set<Vector> getPossibleVelocitiesMinusKnockback() {
        Set<Vector> possibleMovements = new HashSet<>();
        possibleMovements.add(clientVelocity);

        if (clientVelocityOnLadder != null) {
            possibleMovements.add(clientVelocityOnLadder);
        }

        if (clientVelocitySwimHop != null) {
            possibleMovements.add(clientVelocitySwimHop);
        }

        if (clientVelocityFireworkBoost != null) {
            possibleMovements.add(clientVelocityFireworkBoost);
        }

        return possibleMovements;
    }

    public void addTransactionResponse(short transactionID) {
        long millisecondResponse = -10000;

        if (transactionsSent.containsKey(transactionID)) {
            millisecondResponse = System.currentTimeMillis() - transactionsSent.remove(transactionID);
            lastTransactionReceived++;
        } else if (System.currentTimeMillis() - GrimAC.lastReload > 30 * 1000) {
            // The server only sends positive transactions, no negative transactions
            bukkitPlayer.kickPlayer("Invalid packet!");
        }

        //Bukkit.broadcastMessage("Time to response " + millisecondResponse);
    }

    public int getPing() {
        return ((CraftPlayer) bukkitPlayer).getHandle().ping;
    }

    public void baseTickAddVector(Vector vector) {
        baseTickAddition.add(vector);
        clientVelocity.add(vector);

        if (clientVelocityOnLadder != null)
            clientVelocityOnLadder.add(vector);

        if (clientVelocitySwimHop != null)
            clientVelocitySwimHop.add(vector);

        if (clientVelocityFireworkBoost != null)
            clientVelocityFireworkBoost.setX(x);
    }

    public void baseTickSetX(double x) {
        baseTickSet.setX(x);
        clientVelocity.setX(x);

        if (clientVelocityOnLadder != null)
            clientVelocityOnLadder.setX(x);

        if (clientVelocitySwimHop != null)
            clientVelocitySwimHop.setX(x);

        if (clientVelocityFireworkBoost != null)
            clientVelocityFireworkBoost.setX(x);
    }

    public void baseTickSetY(double y) {
        baseTickSet.setY(y);
        clientVelocity.setY(y);

        if (clientVelocityOnLadder != null)
            clientVelocityOnLadder.setY(y);

        if (clientVelocitySwimHop != null)
            clientVelocitySwimHop.setY(y);

        if (clientVelocityFireworkBoost != null)
            clientVelocityFireworkBoost.setX(x);
    }

    public void baseTickSetZ(double z) {
        baseTickSet.setZ(z);
        clientVelocity.setZ(z);

        if (clientVelocityOnLadder != null)
            clientVelocityOnLadder.setZ(z);

        if (clientVelocitySwimHop != null)
            clientVelocitySwimHop.setZ(z);

        if (clientVelocityFireworkBoost != null)
            clientVelocityFireworkBoost.setX(x);
    }

    public boolean isEyeInFluid(Tag tag) {
        return this.fluidOnEyes == tag;
    }
}