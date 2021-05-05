package ac.grim.grimac.player;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.utils.collisions.types.SimpleCollisionBox;
import ac.grim.grimac.utils.data.BoatData;
import ac.grim.grimac.utils.enums.FluidTag;
import ac.grim.grimac.utils.enums.Pose;
import ac.grim.grimac.utils.latency.CompensatedFireworks;
import ac.grim.grimac.utils.latency.CompensatedFlying;
import io.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.utils.vector.Vector3d;
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
    public int entityID;
    public short clientVersion;

    public AtomicInteger taskNumber = new AtomicInteger(0);

    public Vector clientVelocity = new Vector();
    public Vector clientVelocityOnLadder = new Vector();
    public Vector clientVelocitySwimHop = new Vector();
    public Vector clientVelocityFireworkBoostOne = new Vector();
    public Vector clientVelocityFireworkBoostTwo = new Vector();


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
    public boolean isPacketSneaking;
    public boolean isPacketSprinting;

    // Set from the time that the movement packet was received, to be thread safe
    public boolean isSneaking;
    public boolean wasSneaking;
    public boolean isCrouching;
    public boolean isSprinting;
    public boolean lastSprinting;

    public boolean packetFlyingDanger;
    public boolean isFlying;
    // If a player collides with the ground, their flying will be set false after their movement
    // But we need to know if they were flying DURING the movement
    // Thankfully we can 100% recover from this using some logic in PredictionData
    // grimPlayer.onGround && !data.isFlying && grimPlayer.isFlying || data.isFlying;
    // If the player touches the ground and was flying, and now isn't flying - the player was flying during movement
    // Or if the player is flying - the player is flying during movement
    public boolean specialFlying;
    public boolean isSwimming;
    public boolean isClimbing;
    public boolean isFallFlying;
    public double fallDistance;
    public SimpleCollisionBox boundingBox;
    public Pose pose = Pose.STANDING;
    // This has to be done before pose is updated
    public boolean isSlowMovement = false;
    public World playerWorld;

    public double movementSpeed;
    public float jumpAmplifier;
    public float levitationAmplifier;
    public float depthStriderLevel;
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
    public Object2DoubleMap<FluidTag> fluidHeight = new Object2DoubleArrayMap<>(2);
    public boolean wasTouchingWater = false;
    public boolean wasEyeInWater = false;
    public FluidTag fluidOnEyes;

    public HashMap<Integer, Vector3d> teleports = new HashMap<>();

    // Set after checks
    public double lastX;
    public double lastY;
    public double lastZ;
    public float lastXRot;
    public float lastYRot;
    public boolean lastOnGround;
    public boolean horizontalCollision;
    public boolean verticalCollision;
    public boolean lastClimbing;
    public boolean couldSkipTick = false;
    public boolean isJustTeleported = false;

    // Possible inputs into the player's movement thing
    public List<Vector> possibleKnockback = new ArrayList<>();
    public CompensatedFlying compensatedFlying;
    public CompensatedFireworks compensatedFireworks;

    // Keep track of basetick stuff
    public Vector baseTickSet;
    public Vector baseTickAddition;
    public AtomicInteger lastTransactionSent = new AtomicInteger(0);
    public Integer lastTransactionReceived = 0;
    public int movementTransaction = Integer.MIN_VALUE;
    public int timerTransaction = Integer.MIN_VALUE;

    // Determining player ping
    public ConcurrentHashMap<Short, Long> transactionsSent = new ConcurrentHashMap<>();

    public GrimPlayer(Player player) {
        this.bukkitPlayer = player;
        this.playerUUID = player.getUniqueId();
        this.entityID = player.getEntityId();
        this.clientVersion = PacketEvents.get().getPlayerUtils().getClientVersion(player).getProtocolVersion();

        Location loginLocation = player.getLocation();
        lastX = loginLocation.getX();
        lastY = loginLocation.getY();
        lastZ = loginLocation.getZ();

        compensatedFlying = new CompensatedFlying(this);
        compensatedFireworks = new CompensatedFireworks(this);
        packetFlyingDanger = bukkitPlayer.isFlying();
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

        if (clientVelocityFireworkBoostOne != null) {
            possibleMovements.add(clientVelocityFireworkBoostOne);
        }

        if (clientVelocityFireworkBoostTwo != null) {
            possibleMovements.add(clientVelocityFireworkBoostTwo);
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

        if (clientVelocityFireworkBoostOne != null)
            clientVelocityFireworkBoostOne.setX(x);
    }

    public void baseTickSetX(double x) {
        baseTickSet.setX(x);
        clientVelocity.setX(x);

        if (clientVelocityOnLadder != null)
            clientVelocityOnLadder.setX(x);

        if (clientVelocitySwimHop != null)
            clientVelocitySwimHop.setX(x);

        if (clientVelocityFireworkBoostOne != null)
            clientVelocityFireworkBoostOne.setX(x);
    }

    public void baseTickSetY(double y) {
        baseTickSet.setY(y);
        clientVelocity.setY(y);

        if (clientVelocityOnLadder != null)
            clientVelocityOnLadder.setY(y);

        if (clientVelocitySwimHop != null)
            clientVelocitySwimHop.setY(y);

        if (clientVelocityFireworkBoostOne != null)
            clientVelocityFireworkBoostOne.setX(x);
    }

    public void baseTickSetZ(double z) {
        baseTickSet.setZ(z);
        clientVelocity.setZ(z);

        if (clientVelocityOnLadder != null)
            clientVelocityOnLadder.setZ(z);

        if (clientVelocitySwimHop != null)
            clientVelocitySwimHop.setZ(z);

        if (clientVelocityFireworkBoostOne != null)
            clientVelocityFireworkBoostOne.setX(x);
    }

    public float getMaxUpStep() {
        return inVehicle ? 0f : 0.6f;
    }

    public boolean isEyeInFluid(FluidTag tag) {
        return this.fluidOnEyes == tag;
    }
}