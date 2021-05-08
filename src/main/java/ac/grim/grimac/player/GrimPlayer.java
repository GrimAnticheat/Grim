package ac.grim.grimac.player;

import ac.grim.grimac.utils.collisions.types.SimpleCollisionBox;
import ac.grim.grimac.utils.data.BoatData;
import ac.grim.grimac.utils.data.VectorData;
import ac.grim.grimac.utils.enums.FluidTag;
import ac.grim.grimac.utils.enums.Pose;
import ac.grim.grimac.utils.latency.*;
import io.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.utils.vector.Vector3d;
import org.bukkit.Bukkit;
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

    public VectorData predictedVelocity = new VectorData(new Vector(), VectorData.VectorType.Normal);
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
    public boolean isPacketSneakingChange;
    public boolean isPacketSprintingChange;

    // Set from the time that the movement packet was received, to be thread safe
    public boolean isSneaking;
    public boolean wasSneaking;
    public boolean isCrouching;
    public boolean isSprinting;
    public boolean lastSprinting;

    public boolean packetFlyingDanger;
    public boolean isFlying;
    public boolean wasFlying;
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
    // Manage sandwiching packets with transactions
    public boolean originalPacket = true;

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

    // You cannot initialize everything here for some reason
    public CompensatedFlying compensatedFlying;
    public CompensatedFireworks compensatedFireworks;
    public CompensatedKnockback compensatedKnockback;
    public CompensatedExplosion compensatedExplosion;
    public CompensatedWorld compensatedWorld;
    public CompensatedEntities compensatedEntities;

    // Keep track of basetick stuff
    public Vector baseTickSet = new Vector();
    public Vector baseTickAddition = new Vector();
    public AtomicInteger lastTransactionSent = new AtomicInteger(0);
    public int packetLastTransactionReceived = 0;
    public int packetLastTickTransactionReceived = 0;
    public int lastTransactionReceived = 0;
    public int lastLastTransactionReceived = 0;
    public int movementTransaction = Integer.MIN_VALUE;
    public int timerTransaction = Integer.MIN_VALUE;

    // Determining player ping
    public ConcurrentHashMap<Short, Long> transactionsSent = new ConcurrentHashMap<>();

    public Vector firstBreadKB = null;
    public Vector possibleKB = null;

    public Vector firstBreadExplosion = null;
    public List<Vector> possibleExplosion = new ArrayList<>();

    public GrimPlayer(Player player) {
        this.bukkitPlayer = player;
        this.playerUUID = player.getUniqueId();
        this.entityID = player.getEntityId();
        this.clientVersion = PacketEvents.get().getPlayerUtils().getClientVersion(player).getProtocolVersion();

        Location loginLocation = player.getLocation();
        lastX = loginLocation.getX();
        lastY = loginLocation.getY();
        lastZ = loginLocation.getZ();

        packetFlyingDanger = bukkitPlayer.isFlying();
        isFlying = bukkitPlayer.isFlying();
        wasFlying = bukkitPlayer.isFlying();

        compensatedFlying = new CompensatedFlying(this);
        compensatedFireworks = new CompensatedFireworks(this);
        compensatedKnockback = new CompensatedKnockback(this);
        compensatedExplosion = new CompensatedExplosion(this);
        compensatedWorld = new CompensatedWorld(this);
        compensatedEntities = new CompensatedEntities(this);
    }

    public Set<VectorData> getPossibleVelocities() {
        Set<VectorData> set = new HashSet<>();

        if (firstBreadKB != null) {
            set.add(new VectorData(firstBreadKB.clone().add(baseTickAddition), VectorData.VectorType.PossibleKB));
        }

        if (possibleKB != null) {
            // Allow water pushing to affect knockback
            set.add(new VectorData(possibleKB.clone().add(baseTickAddition), VectorData.VectorType.Knockback));
        } else {
            set.addAll(getPossibleVelocitiesMinusKnockback());
            return set;
        }

        return set;
    }

    public Set<VectorData> getPossibleVelocitiesMinusKnockback() {
        Set<VectorData> possibleMovements = new HashSet<>();
        possibleMovements.add(new VectorData(clientVelocity, VectorData.VectorType.Normal));

        if (clientVelocityOnLadder != null) {
            possibleMovements.add(new VectorData(clientVelocityOnLadder, VectorData.VectorType.Ladder));
        }

        if (clientVelocitySwimHop != null) {
            possibleMovements.add(new VectorData(clientVelocitySwimHop, VectorData.VectorType.Swimhop));
        }

        return possibleMovements;
    }

    public void addTransactionResponse(short transactionID) {
        checkTransactionValid(transactionID);
        packetLastTickTransactionReceived++;

        if (!compensatedKnockback.handleTransactionPacket(transactionID) &&
                !compensatedExplosion.handleTransactionPacket(transactionID)) {
            packetLastTickTransactionReceived++;
        }
    }

    // Tested to 20k packets per second per player and couldn't false
    public void checkTransactionValid(short transactionID) {
        //Bukkit.broadcastMessage("Checking transaction " + transactionID + " versus " + packetLastTransactionReceived);
        if (transactionID != ((((packetLastTickTransactionReceived % 32767) * -1) - 1))) {
            Bukkit.broadcastMessage("Not a valid transaction!");
        }
    }

    public int getPing() {
        return ((CraftPlayer) bukkitPlayer).getHandle().ping;
    }

    public short getNextTransactionID() {
        return (short) (-1 * (lastTransactionSent.getAndIncrement() % 32768));
    }

    public void baseTickAddVector(Vector vector) {
        baseTickAddition.add(vector);

        clientVelocity.add(vector);

        if (clientVelocityOnLadder != null)
            clientVelocityOnLadder.add(vector);

        if (clientVelocitySwimHop != null)
            clientVelocitySwimHop.add(vector);
    }

    public void baseTickSetX(double x) {
        baseTickSet.setX(x);
        clientVelocity.setX(x);

        if (clientVelocityOnLadder != null)
            clientVelocityOnLadder.setX(x);

        if (clientVelocitySwimHop != null)
            clientVelocitySwimHop.setX(x);
    }

    public void baseTickSetY(double y) {
        baseTickSet.setY(y);
        clientVelocity.setY(y);

        if (clientVelocityOnLadder != null)
            clientVelocityOnLadder.setY(y);

        if (clientVelocitySwimHop != null)
            clientVelocitySwimHop.setY(y);
    }

    public void baseTickSetZ(double z) {
        baseTickSet.setZ(z);
        clientVelocity.setZ(z);

        if (clientVelocityOnLadder != null)
            clientVelocityOnLadder.setZ(z);

        if (clientVelocitySwimHop != null)
            clientVelocitySwimHop.setZ(z);
    }

    public float getMaxUpStep() {
        return inVehicle ? 0f : 0.6f;
    }

    public boolean isEyeInFluid(FluidTag tag) {
        return this.fluidOnEyes == tag;
    }
}