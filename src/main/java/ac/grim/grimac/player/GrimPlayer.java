package ac.grim.grimac.player;

import ac.grim.grimac.checks.movement.ExplosionHandler;
import ac.grim.grimac.checks.movement.KnockbackHandler;
import ac.grim.grimac.checks.movement.TimerCheck;
import ac.grim.grimac.predictionengine.UncertaintyHandler;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.BoatData;
import ac.grim.grimac.utils.data.PacketStateData;
import ac.grim.grimac.utils.data.VectorData;
import ac.grim.grimac.utils.data.VelocityData;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import ac.grim.grimac.utils.enums.EntityType;
import ac.grim.grimac.utils.enums.FluidTag;
import ac.grim.grimac.utils.enums.Pose;
import ac.grim.grimac.utils.latency.*;
import ac.grim.grimac.utils.math.TrigHandler;
import io.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.utils.pair.Pair;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import io.github.retrooper.packetevents.utils.server.ServerVersion;
import io.github.retrooper.packetevents.utils.vector.Vector3d;
import io.github.retrooper.packetevents.utils.versionlookup.VersionLookupUtils;
import io.github.retrooper.packetevents.utils.versionlookup.v_1_7_10.SpigotVersionLookup_1_7;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

// Everything in this class should be sync'd to the anticheat thread.
// Put variables sync'd to the netty thread in PacketStateData
// Variables that need lag compensation should have their own class
// Soon there will be a generic class for lag compensation
public class GrimPlayer {
    public final UUID playerUUID;
    public final int entityID;
    public final Player bukkitPlayer;
    // Determining player ping
    // The difference between keepalive and transactions is that keepalive is async while transactions are sync
    private final ConcurrentLinkedQueue<Pair<Short, Long>> transactionsSent = new ConcurrentLinkedQueue<>();
    private final ClientVersion clientVersion;
    // This is the most essential value and controls the threading
    public AtomicInteger tasksNotFinished = new AtomicInteger(0);
    public Vector clientVelocity = new Vector();
    public Vector clientVelocityOnLadder = new Vector();
    public Vector clientVelocitySwimHop = new Vector();
    public VectorData predictedVelocity = new VectorData(new Vector(), VectorData.VectorType.Normal);
    public Vector actualMovement = new Vector();
    public Vector stuckSpeedMultiplier = new Vector(1, 1, 1);
    public Vector blockSpeedMultiplier = new Vector(1, 1, 1);
    public Vector lastStuckSpeedMultiplier = new Vector(1, 1, 1);
    public UncertaintyHandler uncertaintyHandler = new UncertaintyHandler();
    public double gravity;
    public float friction;
    public float speed;
    // Set from prediction data
    public double x;
    public double y;
    public double z;
    public float xRot;
    public float yRot;
    public boolean onGround;
    // Set from the time that the movement packet was received, to be thread safe
    public boolean isSneaking;
    public boolean wasSneaking;
    public boolean isCrouching;
    public boolean isSprinting;
    public boolean isUsingItem;
    public boolean lastSprinting;
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
    public boolean isGliding;
    public double fallDistance;
    public SimpleCollisionBox boundingBox;
    public Pose pose = Pose.STANDING;
    // Determining slow movement has to be done before pose is updated
    public boolean isSlowMovement = false;
    public World playerWorld;
    public double movementSpeed;
    public float jumpAmplifier;
    public float levitationAmplifier;
    public float slowFallingAmplifier;
    public float dolphinsGraceAmplifier;
    public float depthStriderLevel;
    public float flySpeed;
    public boolean inVehicle;
    public PacketEntity playerVehicle;
    public float vehicleHorizontal;
    public float vehicleForward;
    public BoatData boatData = new BoatData();
    // We determine this
    public boolean isActuallyOnGround;
    // Set from base tick
    public boolean wasTouchingWater = false;
    public boolean wasTouchingLava = false;
    // For slightly reduced vertical lava friction and jumping
    public boolean slightlyTouchingLava = false;
    // For jumping
    public boolean slightlyTouchingWater = false;
    public boolean wasEyeInWater = false;
    public FluidTag fluidOnEyes;
    public ConcurrentLinkedQueue<Vector3d> teleports = new ConcurrentLinkedQueue<>();
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
    public boolean canGroundRiptide = false;
    // You cannot initialize everything here for some reason
    public CompensatedFlying compensatedFlying;
    public CompensatedFireworks compensatedFireworks;
    public CompensatedRiptide compensatedRiptide;
    public CompensatedElytra compensatedElytra;
    public KnockbackHandler knockbackHandler;
    public ExplosionHandler explosionHandler;
    public CompensatedWorld compensatedWorld;
    public CompensatedEntities compensatedEntities;
    public TrigHandler trigHandler;
    public PacketStateData packetStateData;
    // Keep track of basetick stuff
    public Vector baseTickSet = new Vector();
    public Vector baseTickAddition = new Vector();
    public AtomicInteger lastTransactionSent = new AtomicInteger(0);
    // For syncing together the main thread with the packet thread
    public int lastTransactionAtStartOfTick = 0;
    // For timer checks and fireworks
    public int lastTransactionBeforeLastMovement = 0;
    // For syncing the player's full swing in 1.9+
    public int movementPackets = 0;
    // For setting the player as teleporting on their first tick
    public boolean isFirstTick = true;
    // Sync together block placing/breaking by waiting for the main thread
    // This sucks, but it's the only "real" option
    // Either we have to do the work of the server async to figure out whether a block placed, or we wait for the server to do it
    public VelocityData firstBreadKB = null;
    public VelocityData possibleKB = null;
    public VelocityData firstBreadExplosion = null;
    public VelocityData knownExplosion = null;
    public TimerCheck timerCheck;
    private int transactionPing = 0;
    private long playerClockAtLeast = 0;

    public GrimPlayer(Player player) {
        this.bukkitPlayer = player;
        this.playerUUID = player.getUniqueId();
        this.entityID = player.getEntityId();

        Location loginLocation = player.getLocation();
        lastX = loginLocation.getX();
        lastY = loginLocation.getY();
        lastZ = loginLocation.getZ();

        isFlying = bukkitPlayer.isFlying();
        wasFlying = bukkitPlayer.isFlying();

        // If we have a protocol hack plugin, use it's API to get the player's version
        // Otherwise, if we are using 1.7, use the 1.7 class to get the player's protocol version (built-in hack)
        // Otherwise, the player must be the server's protocol version
        clientVersion = VersionLookupUtils.isDependencyAvailable() ? ClientVersion.getClientVersion(VersionLookupUtils.getProtocolVersion(bukkitPlayer)) :
                PacketEvents.get().getServerUtils().getVersion() == ServerVersion.v_1_7_10 ?
                        ClientVersion.getClientVersion(SpigotVersionLookup_1_7.getProtocolVersion(player)) :
                        ClientVersion.getClientVersion(PacketEvents.get().getServerUtils().getVersion().getProtocolVersion());

        compensatedFlying = new CompensatedFlying(this);
        compensatedFireworks = new CompensatedFireworks(this);
        compensatedRiptide = new CompensatedRiptide(this);
        compensatedElytra = new CompensatedElytra(this);
        knockbackHandler = new KnockbackHandler(this);
        explosionHandler = new ExplosionHandler(this);
        compensatedWorld = new CompensatedWorld(this);
        compensatedEntities = new CompensatedEntities(this);
        trigHandler = new TrigHandler(this);
        timerCheck = new TimerCheck(this);

        packetStateData = new PacketStateData();
        packetStateData.lastSlotSelected = bukkitPlayer.getInventory().getHeldItemSlot();
    }

    public Set<VectorData> getPossibleVelocities() {
        Set<VectorData> set = new HashSet<>();

        if (firstBreadKB != null) {
            set.add(new VectorData(firstBreadKB.vector.clone().add(baseTickAddition), VectorData.VectorType.Knockback));
        }

        if (possibleKB != null) {
            // Allow water pushing to affect knockback
            set.add(new VectorData(possibleKB.vector.clone().add(baseTickAddition), VectorData.VectorType.Knockback));
        }

        set.addAll(getPossibleVelocitiesMinusKnockback());
        return set;
    }

    public Set<VectorData> getPossibleVelocitiesMinusKnockback() {
        Set<VectorData> possibleMovements = new HashSet<>();
        possibleMovements.add(new VectorData(clientVelocity, VectorData.VectorType.Normal));

        if (clientVelocityOnLadder != null) {
            possibleMovements.add(new VectorData(clientVelocityOnLadder, VectorData.VectorType.Climbable));
        }

        if (clientVelocitySwimHop != null) {
            possibleMovements.add(new VectorData(clientVelocitySwimHop, VectorData.VectorType.Swimhop));
        }

        return possibleMovements;
    }

    public void addTransactionSend(short id) {
        transactionsSent.add(new Pair<>(id, System.currentTimeMillis()));
    }

    // Players can get 0 ping by repeatedly sending invalid transaction packets, but that will only hurt them
    // The design is allowing players to miss transaction packets, which shouldn't be possible
    // But if some error made a client miss a packet, then it won't hurt them too bad.
    // Also it forces players to take knockback
    public void addTransactionResponse(short id) {
        Pair<Short, Long> data;
        do {
            data = transactionsSent.poll();
            if (data != null) {
                packetStateData.packetLastTransactionReceived++;
                transactionPing = (int) (System.currentTimeMillis() - data.getSecond());
                playerClockAtLeast = System.currentTimeMillis() - transactionPing;

                knockbackHandler.handleTransactionPacket(data.getFirst());
                explosionHandler.handleTransactionPacket(data.getFirst());
            }
        } while (data != null && data.getFirst() != id);
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
        if (playerVehicle == null) return 0.6f;

        if (playerVehicle.type == EntityType.BOAT) {
            return 0f;
        }

        // Pigs, horses, striders, and other vehicles all have 1 stepping height
        return 1.0f;
    }

    public boolean isEyeInFluid(FluidTag tag) {
        return this.fluidOnEyes == tag;
    }

    public ClientVersion getClientVersion() {
        return clientVersion;
    }

    public int getKeepAlivePing() {
        return PacketEvents.get().getPlayerUtils().getPing(playerUUID);
    }

    public int getTransactionPing() {
        return transactionPing;
    }

    public long getPlayerClockAtLeast() {
        return playerClockAtLeast;
    }
}