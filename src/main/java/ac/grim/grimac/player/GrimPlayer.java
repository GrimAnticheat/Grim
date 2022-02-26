package ac.grim.grimac.player;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.events.packets.CheckManagerListener;
import ac.grim.grimac.manager.CheckManager;
import ac.grim.grimac.manager.SetbackTeleportUtil;
import ac.grim.grimac.manager.init.start.ViaBackwardsManager;
import ac.grim.grimac.predictionengine.MovementCheckRunner;
import ac.grim.grimac.predictionengine.PointThreeEstimator;
import ac.grim.grimac.predictionengine.UncertaintyHandler;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.*;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import ac.grim.grimac.utils.enums.FluidTag;
import ac.grim.grimac.utils.enums.Pose;
import ac.grim.grimac.utils.latency.*;
import ac.grim.grimac.utils.lists.ConcurrentList;
import ac.grim.grimac.utils.math.TrigHandler;
import ac.grim.grimac.utils.nmsutil.GetBoundingBox;
import com.earth2me.essentials.Essentials;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityVelocity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPing;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowConfirmation;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.packet.PacketTracker;
import io.github.retrooper.packetevents.utils.GeyserUtil;
import io.github.retrooper.packetevents.utils.dependencies.viaversion.ViaVersionUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

// Everything in this class should be sync'd to the anticheat thread.
// Put variables sync'd to the netty thread in PacketStateData
// Variables that need lag compensation should have their own class
// Soon there will be a generic class for lag compensation
public class GrimPlayer {
    public final UUID playerUUID;
    public final User user;
    public int entityID;
    @Nullable
    public Player bukkitPlayer;
    // Determining player ping
    // The difference between keepalive and transactions is that keepalive is async while transactions are sync
    public final Queue<Pair<Short, Long>> transactionsSent = new ConcurrentLinkedQueue<>();
    // Sync this to the netty thread because when spamming transactions, they can get out of order... somehow
    public final ConcurrentList<Short> didWeSendThatTrans = new ConcurrentList<>();
    private final AtomicInteger transactionIDCounter = new AtomicInteger(0);
    public boolean sendTrans = true;
    public Vector clientVelocity = new Vector();
    public double lastWasClimbing = 0;
    public boolean canSwimHop = false;
    public int riptideSpinAttackTicks = 0;
    public boolean hasGravity = true;
    public boolean playerEntityHasGravity = true;
    public VectorData predictedVelocity = new VectorData(new Vector(), VectorData.VectorType.Normal);
    public Vector actualMovement = new Vector();
    public Vector stuckSpeedMultiplier = new Vector(1, 1, 1);
    public Vector blockSpeedMultiplier = new Vector(1, 1, 1);
    public UncertaintyHandler uncertaintyHandler;
    public double gravity;
    public float friction;
    public double speed;
    public Vector3d calculatedCollision = new Vector3d();
    public Vector3d filterMojangStupidityOnMojangStupidity = new Vector3d();
    public double x;
    public double y;
    public double z;
    public double lastX;
    public double lastY;
    public double lastZ;
    public float xRot;
    public float yRot;
    public float lastXRot;
    public float lastYRot;
    public boolean onGround;
    public boolean lastOnGround;
    public boolean isSneaking;
    public boolean wasSneaking;
    public boolean isCrouching;
    public boolean isSprinting;
    public boolean lastSprinting;
    // The client updates sprinting attribute at end of each tick
    // Don't false if the server update's the player's sprinting status
    public boolean lastSprintingForSpeed;
    public boolean isFlying;
    public boolean canFly;
    public boolean wasFlying;
    // If a player collides with the ground, their flying will be set false after their movement
    // But we need to know if they were flying DURING the movement
    // Thankfully we can 100% recover from this using some logic in PredictionData
    // If the player touches the ground and was flying, and now isn't flying - the player was flying during movement
    // Or if the player is flying - the player is flying during movement
    public boolean specialFlying;
    public boolean isSwimming;
    public boolean wasSwimming;
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
    public boolean isInBed = false;
    public boolean lastInBed = false;
    public boolean isDead = false;
    public int food = 20;
    public float depthStriderLevel;
    public float flySpeed;
    public VehicleData vehicleData = new VehicleData();
    // The client claims this
    public boolean clientClaimsLastOnGround;
    // Set from base tick
    public boolean wasTouchingWater = false;
    public boolean wasTouchingLava = false;
    // For slightly reduced vertical lava friction and jumping
    public boolean slightlyTouchingLava = false;
    // For jumping
    public boolean slightlyTouchingWater = false;
    public boolean wasEyeInWater = false;
    public FluidTag fluidOnEyes;
    public boolean horizontalCollision;
    public boolean verticalCollision;
    public boolean clientControlledHorizontalCollision;
    public boolean clientControlledVerticalCollision;
    // Okay, this is our 0.03 detection
    //
    // couldSkipTick determines if an input could have resulted in the player skipping a tick < 0.03
    //
    // skippedTickInActualMovement determines if, relative to actual movement, the player didn't move enough
    // and a 0.03 vector was "close enough" to be an accurate prediction
    public boolean couldSkipTick = false;
    // This determines if the
    public boolean skippedTickInActualMovement = false;
    // You cannot initialize everything here for some reason
    public CompensatedFireworks compensatedFireworks;
    public CompensatedWorld compensatedWorld;
    public CompensatedEntities compensatedEntities;
    public CompensatedPotions compensatedPotions;
    public LatencyUtils latencyUtils = new LatencyUtils();
    public PointThreeEstimator pointThreeEstimator;
    public TrigHandler trigHandler;
    public PacketStateData packetStateData;
    // Keep track of basetick stuff
    public Vector baseTickAddition = new Vector();
    public Vector baseTickWaterPushing = new Vector();
    public AtomicInteger lastTransactionSent = new AtomicInteger(0);
    public AtomicInteger lastTransactionReceived = new AtomicInteger(0);
    // For syncing the player's full swing in 1.9+
    public int movementPackets = 0;
    public VelocityData firstBreadKB = null;
    public VelocityData likelyKB = null;
    public VelocityData firstBreadExplosion = null;
    public VelocityData likelyExplosions = null;
    public CheckManager checkManager;
    public MovementCheckRunner movementCheckRunner;
    public boolean tryingToRiptide = false;
    public int minPlayerAttackSlow = 0;
    public int maxPlayerAttackSlow = 0;
    public boolean inVehicle;
    public Integer vehicle = null;
    public PacketEntity playerVehicle;
    public GameMode gamemode;
    public Vector3d bedPosition;
    PacketTracker packetTracker;
    private ClientVersion clientVersion;
    private int transactionPing = 0;
    private long playerClockAtLeast = 0;
    public long lastBlockPlaceUseItem = 0;
    public Queue<PacketWrapper> placeUseItemPackets = new LinkedBlockingQueue<>();
    // This variable is for support with test servers that want to be able to disable grim
    // Grim disabler 2022 still working!
    public boolean disableGrim = false;

    public GrimPlayer(User user) {
        this.playerUUID = user.getProfile().getUUID();
        this.user = user;

        // Geyser players don't have Java movement
        if (GeyserUtil.isGeyserPlayer(playerUUID)) return;

        pollData();

        // We can't send transaction packets to this player, disable the anticheat for them
        if (!ViaBackwardsManager.isViaLegacyUpdated && getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_16_4)) {
            LogUtil.warn(ChatColor.RED + "Please update ViaBackwards to 4.0.2 or newer");
            LogUtil.warn(ChatColor.RED + "An important packet is broken for 1.16 and below clients on this ViaBackwards version");
            LogUtil.warn(ChatColor.RED + "Disabling all checks for 1.16 and below players as otherwise they WILL be falsely banned");
            LogUtil.warn(ChatColor.RED + "Supported version: " + ChatColor.WHITE + "https://www.spigotmc.org/resources/viabackwards.27448/");
            return;
        }

        boundingBox = GetBoundingBox.getBoundingBoxFromPosAndSize(x, y, z, 0.6f, 1.8f);

        if (ViaVersionUtil.isAvailable()) {
            UserConnection connection = Via.getManager().getConnectionManager().getConnectedClient(playerUUID);
            packetTracker = connection != null ? connection.getPacketTracker() : null;
        }

        compensatedWorld = new CompensatedWorld(this);
        compensatedFireworks = new CompensatedFireworks(this);
        compensatedEntities = new CompensatedEntities(this);
        compensatedPotions = new CompensatedPotions(this);
        trigHandler = new TrigHandler(this);
        uncertaintyHandler = new UncertaintyHandler(this);
        pointThreeEstimator = new PointThreeEstimator(this);

        packetStateData = new PacketStateData();

        checkManager = new CheckManager(this);
        movementCheckRunner = new MovementCheckRunner(this);

        uncertaintyHandler.pistonPushing.add(0d);
        uncertaintyHandler.collidingEntities.add(0);

        GrimAPI.INSTANCE.getPlayerDataManager().addPlayer(user, this);
    }

    public Set<VectorData> getPossibleVelocities() {
        Set<VectorData> set = new HashSet<>();

        if (firstBreadKB != null) {
            set.add(new VectorData(firstBreadKB.vector.clone(), VectorData.VectorType.Knockback));
        }

        if (likelyKB != null) {
            // Allow water pushing to affect knockback
            set.add(new VectorData(likelyKB.vector.clone(), VectorData.VectorType.Knockback));
        }

        set.addAll(getPossibleVelocitiesMinusKnockback());
        return set;
    }

    public Set<VectorData> getPossibleVelocitiesMinusKnockback() {
        Set<VectorData> possibleMovements = new HashSet<>();
        possibleMovements.add(new VectorData(clientVelocity, VectorData.VectorType.Normal));

        // A player cannot swim hop (> 0 y vel) and be on the ground
        // Fixes bug with underwater stepping movement being confused with swim hopping movement
        if (canSwimHop && !onGround) {
            possibleMovements.add(new VectorData(clientVelocity.clone().setY(0.3f), VectorData.VectorType.Swimhop));
        }

        // If the player has that client sided riptide thing and has colliding with an entity this tick
        if (riptideSpinAttackTicks >= 0 && uncertaintyHandler.collidingEntities.getLast() > 0) {
            possibleMovements.add(new VectorData(clientVelocity.clone().multiply(-0.2), VectorData.VectorType.Trident));
        }

        if (lastWasClimbing != 0) {
            possibleMovements.add(new VectorData(clientVelocity.clone().setY(lastWasClimbing + baseTickAddition.getY()), VectorData.VectorType.Climbable));
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
                if (data == null)
                    break;

                int incrementingID = lastTransactionReceived.incrementAndGet();
                transactionPing = (int) (System.nanoTime() - data.getSecond());
                playerClockAtLeast = data.getSecond();

                // A transaction means a new tick, so apply any block places
                CheckManagerListener.handleQueuedPlaces(this, false, 0, 0, System.currentTimeMillis());
                latencyUtils.handleNettySyncTransaction(incrementingID);
            } while (data.getFirst() != id);
        }

        // Were we the ones who sent the packet?
        return data != null && data.getFirst() == id;
    }

    public void baseTickAddWaterPushing(Vector vector) {
        baseTickWaterPushing.add(vector);
    }

    public void baseTickAddVector(Vector vector) {
        clientVelocity.add(vector);
        baseTickAddition.add(vector);
    }

    public float getMaxUpStep() {
        if (playerVehicle == null) return 0.6f;

        if (playerVehicle.type == EntityTypes.BOAT) {
            return 0f;
        }

        // Pigs, horses, striders, and other vehicles all have 1 stepping height
        return 1.0f;
    }

    public void sendTransaction() {
        short transactionID = getNextTransactionID(1);
        try {
            addTransactionSend(transactionID);

            if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_17)) {
                user.sendPacket(new WrapperPlayServerPing(transactionID));
            } else {
                user.sendPacket(new WrapperPlayServerWindowConfirmation((byte) 0, transactionID, false));
            }
        } catch (Exception ignored) { // Fix protocollib + viaversion support by ignoring any errors :) // TODO: Fix this
            // recompile
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

    public double getEyeHeight() {
        return pose.eyeHeight;
    }

    public void pollData() {
        if (this.bukkitPlayer == null) {
            this.bukkitPlayer = Bukkit.getPlayer(playerUUID);

            if (this.bukkitPlayer == null) return;

            this.entityID = bukkitPlayer.getEntityId();
            this.entityID = bukkitPlayer.getEntityId();
            this.playerWorld = bukkitPlayer.getWorld();
            this.gamemode = bukkitPlayer.getGameMode();
        }
    }

    public ClientVersion getClientVersion() {
        return user.getClientVersion(); // It's a variable that will get inlined, no map calls.
    }

    public CompensatedInventory getInventory() {
        return (CompensatedInventory) checkManager.getPacketCheck(CompensatedInventory.class);
    }

    public void setVulnerable() {
        // Essentials gives players invulnerability after teleport, which is bad
        try {
            Plugin essentials = Bukkit.getServer().getPluginManager().getPlugin("Essentials");
            if (essentials == null) return;
            if (bukkitPlayer == null) return;

            Object user = ((Essentials) essentials).getUser(bukkitPlayer);
            if (user == null) return;

            // Use reflection because there isn't an API for this
            Field invulnerable = user.getClass().getDeclaredField("teleportInvulnerabilityTimestamp");
            invulnerable.setAccessible(true);
            invulnerable.set(user, 0);
        } catch (Exception e) { // Might error from very outdated Essentials builds
            e.printStackTrace();
        }
    }

    public List<Double> getPossibleEyeHeights() { // We don't return sleeping eye height
        if (getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_14)) { // Elytra, sneaking (1.14), standing
            return Arrays.asList(0.4, 1.27, 1.62);
        } else if (getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9)) { // Elytra, sneaking, standing
            return Arrays.asList(0.4, 1.54, 1.62);
        } else { // Only sneaking or standing
            return Arrays.asList((double) (1.62f - 0.08f), (double) (1.62f));
        }
    }

    public int getTransactionPing() {
        return transactionPing;
    }

    public long getPlayerClockAtLeast() {
        return playerClockAtLeast;
    }

    public SetbackTeleportUtil getSetbackTeleportUtil() {
        return checkManager.getSetbackUtil();
    }

    public boolean wouldCollisionResultFlagGroundSpoof(double inputY, double collisionY) {
        boolean verticalCollision = inputY != collisionY;
        boolean calculatedOnGround = verticalCollision && inputY < 0.0D;

        // We don't care about ground results here
        if (exemptOnGround()) return false;

        // If the player is on the ground with a y velocity of 0, let the player decide (too close to call)
        if (inputY == -SimpleCollisionBox.COLLISION_EPSILON && collisionY > -SimpleCollisionBox.COLLISION_EPSILON && collisionY <= 0)
            return false;

        return calculatedOnGround != onGround;
    }

    public boolean exemptOnGround() {
        return inVehicle
                || uncertaintyHandler.pistonX != 0 || uncertaintyHandler.pistonY != 0
                || uncertaintyHandler.pistonZ != 0 || uncertaintyHandler.isStepMovement
                || isFlying || isDead || isInBed || lastInBed || uncertaintyHandler.lastFlyingStatusChange > -30
                || uncertaintyHandler.lastHardCollidingLerpingEntity > -3 || uncertaintyHandler.isOrWasNearGlitchyBlock;
    }

    public void handleMountVehicle(int vehicleID) {
        compensatedEntities.serverPlayerVehicle = vehicleID;
        // The server does override this with some vehicles. This is intentional.
        user.sendPacket(new WrapperPlayServerEntityVelocity(vehicleID, new Vector3d()));

        // Help prevent transaction split
        sendTransaction();

        latencyUtils.addRealTimeTask(lastTransactionSent.get(), () -> {
            PacketEntity packetVehicle = compensatedEntities.getEntity(vehicleID);
            if (packetVehicle == null) return; // Vanilla behavior for invalid vehicles

            this.vehicle = vehicleID;
            this.playerVehicle = packetVehicle;
            this.inVehicle = true;
            this.vehicleData.wasVehicleSwitch = true;
        });
    }

    public void handleDismountVehicle(PacketSendEvent event) {
        // Help prevent transaction split
        sendTransaction();

        compensatedEntities.serverPlayerVehicle = null;
        event.getPostTasks().add(() -> {
            if (vehicle != null) {
                TrackerData data = compensatedEntities.serverPositionsMap.get(vehicle);
                if (data != null) {
                    user.sendPacket(new WrapperPlayServerEntityTeleport(vehicle, new Vector3d(data.getX(), data.getY(), data.getZ()), data.getXRot(), data.getYRot(), false));
                }
            }
        });

        latencyUtils.addRealTimeTask(lastTransactionSent.get(), () -> {
            this.playerVehicle = null;
            this.vehicle = null;
            this.inVehicle = false;
            this.vehicleData.wasVehicleSwitch = true;
        });
    }
}