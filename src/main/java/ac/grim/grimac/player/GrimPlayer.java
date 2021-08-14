package ac.grim.grimac.player;

import ac.grim.grimac.checks.combat.Reach;
import ac.grim.grimac.checks.movement.ExplosionHandler;
import ac.grim.grimac.checks.movement.KnockbackHandler;
import ac.grim.grimac.checks.movement.NoFall;
import ac.grim.grimac.checks.movement.TimerCheck;
import ac.grim.grimac.predictionengine.UncertaintyHandler;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.*;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import ac.grim.grimac.utils.enums.EntityType;
import ac.grim.grimac.utils.enums.FluidTag;
import ac.grim.grimac.utils.enums.Pose;
import ac.grim.grimac.utils.latency.*;
import ac.grim.grimac.utils.math.TrigHandler;
import ac.grim.grimac.utils.nmsImplementations.XMaterial;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.packet.PacketTracker;
import io.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.packetwrappers.play.out.ping.WrappedPacketOutPing;
import io.github.retrooper.packetevents.packetwrappers.play.out.transaction.WrappedPacketOutTransaction;
import io.github.retrooper.packetevents.utils.list.ConcurrentList;
import io.github.retrooper.packetevents.utils.pair.Pair;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import io.github.retrooper.packetevents.utils.player.Hand;
import io.github.retrooper.packetevents.utils.server.ServerVersion;
import io.github.retrooper.packetevents.utils.vector.Vector3d;
import io.github.retrooper.packetevents.utils.versionlookup.viaversion.ViaVersionLookupUtils;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
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
    public final ConcurrentLinkedQueue<Pair<Short, Long>> transactionsSent = new ConcurrentLinkedQueue<>();
    // Sync this to the netty thread because when spamming transactions, they can get out of order... somehow
    public final ConcurrentList<Short> didWeSendThatTrans = new ConcurrentList<>();
    // This is the most essential value and controls the threading
    public AtomicInteger tasksNotFinished = new AtomicInteger(0);
    public PredictionData nextTaskToRun;
    public Vector clientVelocity = new Vector();
    public double lastWasClimbing = 0;
    public boolean canSwimHop = false;
    public int riptideSpinAttackTicks = 0;
    public VectorData predictedVelocity = new VectorData(new Vector(), VectorData.VectorType.Normal);
    public Vector actualMovement = new Vector();
    public Vector stuckSpeedMultiplier = new Vector(1, 1, 1);
    public Vector blockSpeedMultiplier = new Vector(1, 1, 1);
    public UncertaintyHandler uncertaintyHandler;
    public double gravity;
    public float friction;
    public double speed;
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
    public Hand lastHand = Hand.MAIN_HAND;
    public int lastSlotSelected = 0;
    public int ticksSinceLastSlotSwitch = 0;
    public AlmostBoolean isUsingItem;
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
    public boolean wasGliding;
    public boolean isRiptidePose = false;
    public double fallDistance;
    public SimpleCollisionBox boundingBox;
    public Pose pose = Pose.STANDING;
    // Determining slow movement has to be done before pose is updated
    public boolean isSlowMovement = false;
    public World playerWorld;
    public int jumpAmplifier;
    public int levitationAmplifier;
    public int slowFallingAmplifier;
    public int dolphinsGraceAmplifier;
    public float depthStriderLevel;
    public float flySpeed;
    public boolean inVehicle;
    public Integer vehicle = null;
    public PacketEntity playerVehicle;
    public PacketEntity lastVehicle;
    public int lastVehicleSwitch = 1000;
    public boolean lastDummy = false;
    public float vehicleHorizontal = 0f;
    public float vehicleForward = 0f;
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
    public ConcurrentLinkedQueue<Pair<Integer, Vector3d>> teleports = new ConcurrentLinkedQueue<>();
    public ConcurrentLinkedQueue<Pair<Integer, Vector3d>> vehicleTeleports = new ConcurrentLinkedQueue<>();
    // Set after checks
    public double lastX;
    public double lastY;
    public double lastZ;
    public float lastXRot;
    public float lastYRot;
    public boolean lastOnGround;
    public boolean horizontalCollision;
    public boolean verticalCollision;
    public boolean clientControlledHorizontalCollision;
    public boolean clientControlledVerticalCollision;
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
    public CompensatedEating compensatedEating;
    public CompensatedPotions compensatedPotions;
    public TrigHandler trigHandler;
    public PacketStateData packetStateData;
    // Keep track of basetick stuff
    public Vector baseTickAddition = new Vector();
    public AtomicInteger lastTransactionSent = new AtomicInteger(0);
    private final AtomicInteger transactionIDCounter = new AtomicInteger(0);
    // For syncing together the main thread with the packet thread
    public int lastTransactionAtStartOfTick = 0;
    // For timer checks and fireworks
    public int lastTransactionReceived = 0;
    // For syncing the player's full swing in 1.9+
    public int movementPackets = 0;
    // Sync together block placing/breaking by waiting for the main thread
    // This sucks, but it's the only "real" option
    // Either we have to do the work of the server async to figure out whether a block placed, or we wait for the server to do it
    public VelocityData firstBreadKB = null;
    public VelocityData likelyKB = null;
    public VelocityData firstBreadExplosion = null;
    public VelocityData likelyExplosions = null;
    public TimerCheck timerCheck;
    public Reach reach;
    public NoFall noFall;
    public float horseJump = 0;
    public boolean horseJumping = false;
    public boolean tryingToRiptide = false;
    public int minPlayerAttackSlow = 0;
    public int maxPlayerAttackSlow = 0;
    PacketTracker packetTracker;
    private ClientVersion clientVersion;
    private int transactionPing = 0;
    private long playerClockAtLeast = 0;

    public GrimPlayer(Player player) {
        this.bukkitPlayer = player;
        this.playerUUID = player.getUniqueId();
        this.entityID = player.getEntityId();
        this.playerWorld = player.getWorld();

        Location loginLocation = player.getLocation();
        lastX = loginLocation.getX();
        lastY = loginLocation.getY();
        lastZ = loginLocation.getZ();

        isFlying = bukkitPlayer.isFlying();
        wasFlying = bukkitPlayer.isFlying();

        clientVersion = PacketEvents.get().getPlayerUtils().getClientVersion(bukkitPlayer);

        if (ViaVersionLookupUtils.isAvailable()) {
            UserConnection connection = Via.getManager().getConnectionManager().getConnectedClient(playerUUID);
            packetTracker = connection != null ? connection.getPacketTracker() : null;
        }

        if (XMaterial.isNewVersion()) {
            compensatedWorld = new CompensatedWorldFlat(this);
        } else {
            compensatedWorld = new CompensatedWorld(this);
        }

        compensatedFlying = new CompensatedFlying(this);
        compensatedFireworks = new CompensatedFireworks(this);
        compensatedRiptide = new CompensatedRiptide(this);
        compensatedElytra = new CompensatedElytra(this);
        knockbackHandler = new KnockbackHandler(this);
        explosionHandler = new ExplosionHandler(this);
        compensatedEntities = new CompensatedEntities(this);
        compensatedEating = new CompensatedEating(this);
        compensatedPotions = new CompensatedPotions(this);
        trigHandler = new TrigHandler(this);
        timerCheck = new TimerCheck(this);
        reach = new Reach(this);
        noFall = new NoFall(this);
        uncertaintyHandler = new UncertaintyHandler(this);

        packetStateData = new PacketStateData();
        packetStateData.lastSlotSelected = bukkitPlayer.getInventory().getHeldItemSlot();
    }

    public Set<VectorData> getPossibleVelocities() {
        Set<VectorData> set = new HashSet<>();

        if (firstBreadKB != null) {
            set.add(new VectorData(firstBreadKB.vector.clone().add(baseTickAddition), VectorData.VectorType.Knockback));
        }

        if (likelyKB != null) {
            // Allow water pushing to affect knockback
            set.add(new VectorData(likelyKB.vector.clone().add(baseTickAddition), VectorData.VectorType.Knockback));
        }

        set.addAll(getPossibleVelocitiesMinusKnockback());
        return set;
    }

    public Set<VectorData> getPossibleVelocitiesMinusKnockback() {
        Set<VectorData> possibleMovements = new HashSet<>();
        possibleMovements.add(new VectorData(clientVelocity, VectorData.VectorType.Normal));

        if (canSwimHop) {
            possibleMovements.add(new VectorData(clientVelocity.clone().setY(0.3f), VectorData.VectorType.Swimhop));
        }

        // If the player has that client sided riptide thing and has colliding with an entity this tick
        if (riptideSpinAttackTicks >= 0 && uncertaintyHandler.collidingEntities.getLast() > 0) {
            possibleMovements.add(new VectorData(clientVelocity.clone().multiply(-0.2), VectorData.VectorType.Trident));
        }

        if (lastWasClimbing != 0) {
            possibleMovements.add(new VectorData(clientVelocity.clone().setY(lastWasClimbing), VectorData.VectorType.Climbable));
        }

        // Knockback takes precedence over piston pushing in my testing
        // It's very difficult to test precedence so if there's issues with this bouncy implementation let me know
        for (VectorData data : new HashSet<>(possibleMovements)) {
            for (BlockFace direction : uncertaintyHandler.slimePistonBounces) {
                if (direction.getModX() != 0) {
                    possibleMovements.add(data.returnNewModified(data.vector.clone().setX(direction.getModX()), VectorData.VectorType.SlimePistonBounce));
                } else if (direction.getModY() != 0) {
                    possibleMovements.add(data.returnNewModified(data.vector.clone().setY(direction.getModY()), VectorData.VectorType.SlimePistonBounce));
                } else if (direction.getModZ() != 0) {
                    possibleMovements.add(data.returnNewModified(data.vector.clone().setZ(direction.getModZ()), VectorData.VectorType.SlimePistonBounce));
                }
            }
        }

        return possibleMovements;
    }

    // Players can get 0 ping by repeatedly sending invalid transaction packets, but that will only hurt them
    // The design is allowing players to miss transaction packets, which shouldn't be possible
    // But if some error made a client miss a packet, then it won't hurt them too bad.
    // Also it forces players to take knockback
    public boolean addTransactionResponse(short id) {
        // Disable ViaVersion packet limiter
        // Required as ViaVersion listens before us for converting packets between game versions
        if (packetTracker != null)
            packetTracker.setIntervalPackets(0);

        Pair<Short, Long> data = null;
        boolean hasID = false;
        for (Pair<Short, Long> iterator : transactionsSent) {
            if (iterator.getFirst() == id) {
                hasID = true;
                break;
            }
        }

        if (hasID) {
            do {
                data = transactionsSent.poll();
                if (data != null) {
                    int incrementingID = packetStateData.packetLastTransactionReceived.incrementAndGet();
                    transactionPing = (int) (System.nanoTime() - data.getSecond());
                    playerClockAtLeast = System.nanoTime() - transactionPing;

                    // Must be here as this is required to be real time
                    compensatedEating.handleTransactionPacket(incrementingID);
                    reach.handleTransaction(incrementingID);
                    compensatedEntities.handleTransaction(incrementingID);
                }
            } while (data != null && data.getFirst() != id);
        }

        // Were we the ones who sent the packet?
        return data != null && data.getFirst() == id;
    }

    public void baseTickAddVector(Vector vector) {
        baseTickAddition.add(vector);
        clientVelocity.add(vector);
    }

    public float getMaxUpStep() {
        if (playerVehicle == null) return 0.6f;

        if (playerVehicle.type == EntityType.BOAT) {
            return 0f;
        }

        // Pigs, horses, striders, and other vehicles all have 1 stepping height
        return 1.0f;
    }

    public void sendAndFlushTransactionOrPingPong() {
        sendTransactionOrPingPong(getNextTransactionID(1), true);
    }

    // Shouldn't error, but be on the safe side as this is networking stuff
    public void sendTransactionOrPingPong(short transactionID, boolean flush) {
        try {
            addTransactionSend(transactionID);

            if (ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_17)) {
                PacketEvents.get().getPlayerUtils().sendPacket(bukkitPlayer, new WrappedPacketOutPing(transactionID));
            } else {
                PacketEvents.get().getPlayerUtils().sendPacket(bukkitPlayer, new WrappedPacketOutTransaction(0, transactionID, false));
            }

            if (flush)
                PacketEvents.get().getPlayerUtils().flushPackets(bukkitPlayer);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public short getNextTransactionID(int add) {
        // Take the 15 least significant bits, multiply by 1.
        // Short range is -32768 to 32767
        // We return a range of -32767 to 0
        // Allowing a range of -32768 to 0 for velocity + explosions
        return (short) (-1 * (transactionIDCounter.getAndAdd(add) & 0x7FFF));
    }

    public void addTransactionSend(short id) {
        didWeSendThatTrans.add(id);
    }

    public boolean isEyeInFluid(FluidTag tag) {
        return this.fluidOnEyes == tag;
    }

    public ClientVersion getClientVersion() {
        // There seems to be some issues with getting client version on 1.8 with ViaVersion early on join?
        if (clientVersion.getProtocolVersion() == -1) {
            clientVersion = PacketEvents.get().getPlayerUtils().getClientVersion(bukkitPlayer);
        }
        return clientVersion;
    }

    public int getKeepAlivePing() {
        return PacketEvents.get().getPlayerUtils().getPing(bukkitPlayer);
    }

    public int getTransactionPing() {
        return transactionPing;
    }

    public long getPlayerClockAtLeast() {
        return playerClockAtLeast;
    }
}