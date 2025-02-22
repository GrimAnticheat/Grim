package ac.grim.grimac.player;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.AbstractCheck;
import ac.grim.grimac.api.GrimUser;
import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.api.feature.FeatureManager;
import ac.grim.grimac.api.handler.ResyncHandler;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.impl.aim.processor.AimProcessor;
import ac.grim.grimac.checks.impl.misc.ClientBrand;
import ac.grim.grimac.checks.impl.misc.TransactionOrder;
import ac.grim.grimac.events.packets.CheckManagerListener;
import ac.grim.grimac.manager.*;
import ac.grim.grimac.manager.player.features.FeatureManagerImpl;
import ac.grim.grimac.manager.player.handlers.BukkitResyncHandler;
import ac.grim.grimac.predictionengine.MovementCheckRunner;
import ac.grim.grimac.predictionengine.PointThreeEstimator;
import ac.grim.grimac.predictionengine.UncertaintyHandler;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import ac.grim.grimac.utils.anticheat.update.BlockBreak;
import ac.grim.grimac.utils.change.PlayerBlockHistory;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.*;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import ac.grim.grimac.utils.data.packetentity.PacketEntitySelf;
import ac.grim.grimac.utils.data.tags.SyncedTags;
import ac.grim.grimac.utils.enums.FluidTag;
import ac.grim.grimac.utils.enums.Pose;
import ac.grim.grimac.utils.latency.*;
import ac.grim.grimac.utils.math.GrimMath;
import ac.grim.grimac.utils.math.TrigHandler;
import ac.grim.grimac.utils.nmsutil.BlockProperties;
import ac.grim.grimac.utils.nmsutil.GetBoundingBox;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.netty.channel.ChannelHelper;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemEquippable;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.protocol.world.dimension.DimensionType;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.packet.PacketTracker;
import io.github.retrooper.packetevents.adventure.serializer.legacy.LegacyComponentSerializer;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import io.github.retrooper.packetevents.util.viaversion.ViaVersionUtil;
import io.netty.channel.Channel;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

// Everything in this class should be sync'd to the anticheat thread.
// Put variables sync'd to the netty thread in PacketStateData
// Variables that need lag compensation should have their own class
// Soon there will be a generic class for lag compensation
public class GrimPlayer implements GrimUser {
    public UUID uuid;
    public final User user;
    public int entityID;
    @Nullable
    public Player bukkitPlayer;
    // Start transaction handling stuff
    // Determining player ping
    // The difference between keepalive and transactions is that keepalive is async while transactions are sync
    public final Queue<Pair<Short, Long>> transactionsSent = new ConcurrentLinkedQueue<>();
    public final Set<Short> didWeSendThatTrans = ConcurrentHashMap.newKeySet();
    private final AtomicInteger transactionIDCounter = new AtomicInteger(0);
    public AtomicInteger lastTransactionSent = new AtomicInteger(0);
    public AtomicInteger lastTransactionReceived = new AtomicInteger(0);
    // End transaction handling stuff
    // Manager like classes
    public CheckManager checkManager;
    public ActionManager actionManager;
    public PunishmentManager punishmentManager;
    public MovementCheckRunner movementCheckRunner;
    public SyncedTags tagManager;
    // End manager like classes
    public Vector clientVelocity = new Vector();
    PacketTracker packetTracker;
    private long transactionPing = 0;
    public long lastTransSent = 0;
    public long lastTransReceived = 0;
    @Getter
    private long playerClockAtLeast = System.nanoTime();
    public double lastWasClimbing = 0;
    public boolean canSwimHop = false;
    public int riptideSpinAttackTicks = 0;
    public int powderSnowFrozenTicks = 0;
    public boolean hasGravity = true;
    public final long joinTime = System.currentTimeMillis();
    public boolean playerEntityHasGravity = true;
    public VectorData predictedVelocity = new VectorData(new Vector(), VectorData.VectorType.Normal);
    public Vector actualMovement = new Vector();
    public Vector stuckSpeedMultiplier = new Vector(1, 1, 1);
    public UncertaintyHandler uncertaintyHandler;
    public double gravity;
    public float friction;
    public double speed;
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
    public boolean isSprinting;
    public boolean lastSprinting;
    // The client updates sprinting attribute at end of each tick
    // Don't false if the server update's the player's sprinting status
    public boolean lastSprintingForSpeed;
    public boolean isFlying;
    public boolean canFly;
    public boolean wasFlying;
    public boolean isSwimming;
    public boolean wasSwimming;
    public boolean isClimbing;
    public boolean isGliding;
    public boolean wasGliding;
    public boolean isRiptidePose = false;
    public double fallDistance;
    public SimpleCollisionBox boundingBox;
    public Pose pose = Pose.STANDING;
    public Pose lastPose = Pose.STANDING;
    // Determining slow movement has to be done before pose is updated
    public boolean isSlowMovement = false;
    public boolean isInBed = false;
    public boolean lastInBed = false;
    public int food = 20;
    public float depthStriderLevel;
    public float sneakingSpeedMultiplier = 0.3f;
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
    public LastInstanceManager lastInstanceManager;
    public final CompensatedFireworks fireworks;
    public final CompensatedWorld compensatedWorld;
    public final CompensatedEntities compensatedEntities;
    public LatencyUtils latencyUtils;
    public PointThreeEstimator pointThreeEstimator;
    public TrigHandler trigHandler;
    public PacketStateData packetStateData;
    // Keep track of basetick stuff
    public Vector baseTickAddition = new Vector();
    public Vector baseTickWaterPushing = new Vector();
    public Vector startTickClientVel = new Vector();
    // For syncing the player's full swing in 1.9+
    public int movementPackets = 0;
    public VelocityData firstBreadKB = null;
    public VelocityData likelyKB = null;
    public VelocityData firstBreadExplosion = null;
    public VelocityData likelyExplosions = null;
    public int minAttackSlow = 0;
    public int maxAttackSlow = 0;
    public GameMode gamemode;
    public DimensionType dimensionType;
    public Vector3d bedPosition;
    public long lastBlockPlaceUseItem = 0;
    public long lastBlockBreak = 0;
    public AtomicInteger cancelledPackets = new AtomicInteger(0);
    public MainSupportingBlockData mainSupportingBlockData = new MainSupportingBlockData(null, false);
    // possibleEyeHeights[0] = Standing eye heights, [1] = Sneaking. [2] = Elytra, Swimming, and Riptide Trident which only exists in 1.9+
    public double[][] possibleEyeHeights = new double[3][];
    public int totalFlyingPacketsSent;
    public Queue<BlockPlaceSnapshot> placeUseItemPackets = new LinkedBlockingQueue<>();
    public Queue<BlockBreak> queuedBreaks = new LinkedBlockingQueue<>();
    public PlayerBlockHistory blockHistory = new PlayerBlockHistory();
    // This variable is for support with test servers that want to be able to disable grim
    // Grim disabler 2022 still working!
    public boolean disableGrim = false;

    public GrimPlayer(User user) {
        this.user = user;
        this.uuid = user.getUUID();

        boundingBox = GetBoundingBox.getBoundingBoxFromPosAndSizeRaw(x, y, z, 0.6f, 1.8f);

        fireworks = new CompensatedFireworks(this); // Must be before checkmanager

        lastInstanceManager = new LastInstanceManager(this);
        actionManager = new ActionManager(this);
        checkManager = new CheckManager(this);
        punishmentManager = new PunishmentManager(this);
        tagManager = new SyncedTags(this);
        movementCheckRunner = new MovementCheckRunner(this);

        compensatedWorld = new CompensatedWorld(this);
        compensatedEntities = new CompensatedEntities(this);
        latencyUtils = new LatencyUtils(this);
        trigHandler = new TrigHandler(this);
        uncertaintyHandler = new UncertaintyHandler(this); // must be after checkmanager
        pointThreeEstimator = new PointThreeEstimator(this);

        packetStateData = new PacketStateData();

        uncertaintyHandler.riptideEntities.add(0);
        uncertaintyHandler.collidingEntities.add(0);

        if (getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_14)) {
            final float scale = (float) compensatedEntities.self.getAttributeValue(Attributes.SCALE);
            possibleEyeHeights[2] = new double[]{0.4 * scale, 1.62 * scale, 1.27 * scale}; // Elytra, standing, sneaking (1.14)
            possibleEyeHeights[1] = new double[]{1.27 * scale, 1.62 * scale, 0.4 * scale}; // sneaking (1.14), standing, Elytra
            possibleEyeHeights[0] = new double[]{1.62 * scale, 1.27 * scale, 0.4 * scale}; // standing, sneaking (1.14), Elytra
        } else if (getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9)) { // standing, sneaking Elytra
            possibleEyeHeights[2] = new double[]{0.4, 1.62, 1.54}; // Elytra, standing, sneaking (1.13)
            possibleEyeHeights[1] = new double[]{1.54, 1.62, 0.4}; // sneaking (1.9-1.13), standing, Elytra
            possibleEyeHeights[0] = new double[]{1.62, 1.54, 0.4}; // standing, sneaking (1.9-1.13), Elytra
        } else {
            possibleEyeHeights[1] = new double[]{(double) (1.62f - 0.08f), (double) (1.62f)}; // sneaking, standing
            possibleEyeHeights[0] = new double[]{(double) (1.62f), (double) (1.62f - 0.08f)}; // standing, sneaking
        }

        // reload last
        reload();
    }

    public void onPacketCancel() {
        if (spamThreshold != -1 && cancelledPackets.incrementAndGet() > spamThreshold) {
            LogUtil.info("Disconnecting " + getName() + " for spamming invalid packets, packets cancelled within a second " + cancelledPackets);
            disconnect(MessageUtil.miniMessage(MessageUtil.replacePlaceholders(this, GrimAPI.INSTANCE.getConfigManager().getDisconnectClosed())));
            cancelledPackets.set(0);

            if (debugPacketCancel) {
                try {
                    throw new Exception();
                } catch (Exception e) {
                    LogUtil.error("Stacktrace for onPacketCancel (debug-packet-cancel=true)", e);
                }
            }
        }
    }

    public Set<VectorData> getPossibleVelocities() {
        Set<VectorData> set = new HashSet<>();

        if (firstBreadKB != null) {
            set.add(new VectorData(firstBreadKB.vector.clone(), VectorData.VectorType.Knockback).returnNewModified(VectorData.VectorType.FirstBreadKnockback));
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

        // If the player has that client sided riptide thing and has colliding with an entity
        // This was determined in the previous tick but whatever just include the 2 ticks around it
        // for a bit of safety as I doubt people will try to bypass this, it would be a very useless cheat
        if (riptideSpinAttackTicks >= 0 && Collections.max(uncertaintyHandler.riptideEntities) > 0) {
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
        Pair<Short, Long> data = null;
        boolean hasID = false;
        int skipped = 0;
        for (Pair<Short, Long> iterator : transactionsSent) {
            if (iterator.first() == id) {
                hasID = true;
                break;
            }
            skipped++;
        }

        if (hasID) {
            // Transactions that we send don't count towards total limit
            if (packetTracker != null) packetTracker.setIntervalPackets(packetTracker.getIntervalPackets() - 1);

            if (skipped > 0 && System.currentTimeMillis() - joinTime > 5000)
                checkManager.getPacketCheck(TransactionOrder.class).flagAndAlert("skipped: " + skipped);

            do {
                data = transactionsSent.poll();
                if (data == null)
                    break;

                lastTransactionReceived.incrementAndGet();
                lastTransReceived = System.currentTimeMillis();
                transactionPing = (System.nanoTime() - data.second());
                playerClockAtLeast = data.second();
            } while (data.first() != id);

            // A transaction means a new tick, so handle any block interactions
            CheckManagerListener.handleQueuedPlaces(this, false, 0, 0, System.currentTimeMillis());
            CheckManagerListener.handleQueuedBreaks(this, false, 0, 0, System.currentTimeMillis());
            latencyUtils.handleNettySyncTransaction(lastTransactionReceived.get());
        }

        // Were we the ones who sent the packet?
        return data != null;
    }

    public void baseTickAddWaterPushing(Vector vector) {
        baseTickWaterPushing.add(vector);
    }

    public void baseTickAddVector(Vector vector) {
        clientVelocity.add(vector);
    }

    public void trackBaseTickAddition(Vector vector) {
        baseTickAddition.add(vector);
    }

    public float getMaxUpStep() {
        final PacketEntitySelf self = compensatedEntities.self;
        final PacketEntity riding = self.getRiding();
        if (riding == null) return (float) self.getAttributeValue(Attributes.STEP_HEIGHT);

        if (riding.isBoat()) {
            return 0f;
        }

        // Pigs, horses, striders, and other vehicles all have 1 stepping height by default
        return (float) riding.getAttributeValue(Attributes.STEP_HEIGHT);
    }

    public void sendTransaction() {
        sendTransaction(false);
    }

    public void sendTransaction(boolean async) {
        // don't send transactions outside PLAY phase
        // Sending in non-play corrupts the pipeline, don't waste bandwidth when anticheat disabled
        if (user.getEncoderState() != ConnectionState.PLAY) return;

        // Send a packet once every 15 seconds to avoid any memory leaks
        if (disableGrim && (System.nanoTime() - getPlayerClockAtLeast()) > 15e9) {
            return;
        }

        lastTransSent = System.currentTimeMillis();
        short transactionID = (short) (-1 * (transactionIDCounter.getAndIncrement() & 0x7FFF));
        try {

            PacketWrapper<?> packet;
            if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_17)) {
                packet = new WrapperPlayServerPing(transactionID);
            } else {
                packet = new WrapperPlayServerWindowConfirmation((byte) 0, transactionID, false);
            }

            if (async) {
                ChannelHelper.runInEventLoop(user.getChannel(), () -> {
                    addTransactionSend(transactionID);
                    user.writePacket(packet);
                });
            } else {
                addTransactionSend(transactionID);
                user.writePacket(packet);
            }
        } catch (Exception ignored) { // Fix protocollib + viaversion support by ignoring any errors :) // TODO: Fix this
            // recompile
        }
    }

    public void addTransactionSend(short id) {
        didWeSendThatTrans.add(id);
    }

    public boolean isEyeInFluid(FluidTag tag) {
        return this.fluidOnEyes == tag;
    }

    public double getEyeHeight() {
        return getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9) ? pose.eyeHeight
                : isSneaking ? 1.54f : 1.62f;
    }

    public void timedOut() {
        disconnect(MessageUtil.miniMessage(MessageUtil.replacePlaceholders(this, GrimAPI.INSTANCE.getConfigManager().getDisconnectTimeout())));
    }

    public void disconnect(Component reason) {
        String textReason;
        if (reason instanceof TranslatableComponent translatableComponent) {
            textReason = translatableComponent.key();
        } else {
            textReason = LegacyComponentSerializer.legacySection().serialize(reason);
        }
        LogUtil.info("Disconnecting " + user.getProfile().getName() + " for " + ChatColor.stripColor(textReason));
        try {
            user.sendPacket(new WrapperPlayServerDisconnect(reason));
        } catch (Exception ignored) { // There may (?) be an exception if the player is in the wrong state...
            LogUtil.warn("Failed to send disconnect packet to disconnect " + user.getProfile().getName() + "! Disconnecting anyways.");
        }
        user.closeConnection();
        if (bukkitPlayer != null) {
            FoliaScheduler.getEntityScheduler().execute(bukkitPlayer, GrimAPI.INSTANCE.getPlugin(), () -> {
                bukkitPlayer.kickPlayer(textReason);
            }, null, 1);
        }
    }

    public void pollData() {
        // Send a transaction at least once a tick, for timer and post check purposes
        // Don't be the first to send the transaction, or we will stack overflow
        //
        // This will only really activate if there's no entities around the player being tracked
        // 80 is a magic value that is roughly every other tick, we don't want to spam too many packets.
        if (lastTransSent != 0 && lastTransSent + 80 < System.currentTimeMillis()) {
            sendTransaction(true); // send on netty thread
        }
        if ((System.nanoTime() - getPlayerClockAtLeast()) > maxTransactionTime * 1e9) {
            timedOut();
        }

        if (!GrimAPI.INSTANCE.getPlayerDataManager().shouldCheck(user)) {
            GrimAPI.INSTANCE.getPlayerDataManager().remove(user);
        }

        if (packetTracker == null && ViaVersionUtil.isAvailable() && uuid != null) {
            UserConnection connection = Via.getManager().getConnectionManager().getConnectedClient(uuid);
            packetTracker = connection != null ? connection.getPacketTracker() : null;
        }

        if (uuid != null && this.bukkitPlayer == null) {
            this.bukkitPlayer = Bukkit.getPlayer(uuid);
            updatePermissions();
        }
    }

    public void updateVelocityMovementSkipping() {
        if (!couldSkipTick) {
            couldSkipTick = pointThreeEstimator.determineCanSkipTick(BlockProperties.getFrictionInfluencedSpeed((float) (speed * (isSprinting ? 1.3 : 1)), this), getPossibleVelocitiesMinusKnockback());
        }

        Set<VectorData> knockback = new HashSet<>();
        if (firstBreadKB != null) knockback.add(new VectorData(firstBreadKB.vector, VectorData.VectorType.Knockback));
        if (likelyKB != null) knockback.add(new VectorData(likelyKB.vector, VectorData.VectorType.Knockback));

        boolean kbPointThree = pointThreeEstimator.determineCanSkipTick(BlockProperties.getFrictionInfluencedSpeed((float) (speed * (isSprinting ? 1.3 : 1)), this), knockback);
        checkManager.getKnockbackHandler().setPointThree(kbPointThree);

        Set<VectorData> explosion = new HashSet<>();
        if (firstBreadExplosion != null)
            explosion.add(new VectorData(firstBreadExplosion.vector, VectorData.VectorType.Explosion));
        if (likelyExplosions != null)
            explosion.add(new VectorData(likelyExplosions.vector, VectorData.VectorType.Explosion));

        boolean explosionPointThree = pointThreeEstimator.determineCanSkipTick(BlockProperties.getFrictionInfluencedSpeed((float) (speed * (isSprinting ? 1.3 : 1)), this), explosion);
        checkManager.getExplosionHandler().setPointThree(explosionPointThree);

        if (kbPointThree || explosionPointThree) {
            uncertaintyHandler.lastPointThree.reset();
        }
    }

    public boolean noModifyPacketPermission = false;
    public boolean noSetbackPermission = false;

    //TODO: Create a configurable timer for this
    @Override
    public void updatePermissions() {
        if (bukkitPlayer == null) return;
        this.noModifyPacketPermission = bukkitPlayer.hasPermission("grim.nomodifypacket");
        this.noSetbackPermission = bukkitPlayer.hasPermission("grim.nosetback");
        FoliaScheduler.getAsyncScheduler().runNow(GrimAPI.INSTANCE.getPlugin(), t -> {
            for (AbstractCheck check : checkManager.allChecks.values()) {
                if (check instanceof Check) {
                    ((Check) check).updatePermissions();
                }
            }
        });
    }

    private boolean debugPacketCancel = false;
    private int spamThreshold = 100;

    public boolean isPointThree() {
        return getClientVersion().isOlderThan(ClientVersion.V_1_18_2);
    }

    public double getMovementThreshold() {
        return isPointThree() ? 0.03 : 0.0002;
    }

    public ClientVersion getClientVersion() {
        ClientVersion ver = user.getClientVersion();
        // If temporarily null, assume server version...
        return Objects.requireNonNullElseGet(ver, () -> ClientVersion.getById(PacketEvents.getAPI().getServerManager().getVersion().getProtocolVersion()));
    }

    // Alright, someone at mojang decided to not send a flying packet every tick with 1.9
    // Thanks for wasting my time to save 1 MB an hour
    //
    // MEANING, to get an "acceptable" 1.9+ reach check, we must only treat it like a 1.8 clients
    // when it is acting like one and sending a packet every tick.
    //
    // There are two predictable scenarios where this happens:
    // 1. The player moves more than 0.03/0.0002 blocks every tick
    //     - This code runs after the prediction engine to prevent a false when immediately switching back to 1.9-like movements
    //     - 3 ticks is a magic value, but it should buffer out incorrect predictions somewhat.
    // 2. The player is in a vehicle
    public boolean isTickingReliablyFor(int ticks) {
        // 1.21.2+: Tick end packet, on servers 1.21.2+
        // 1.8-: Flying packet
        return !canSkipTicks() || (inVehicle()
                || !uncertaintyHandler.lastPointThree.hasOccurredSince(ticks))
                && !uncertaintyHandler.lastVehicleSwitch.hasOccurredSince(1);
    }

    public boolean inVehicle() {
        return compensatedEntities.self.inVehicle();
    }

    public CompensatedInventory getInventory() {
        return checkManager.getInventory();
    }

    public double[] getPossibleEyeHeights() { // We don't return sleeping eye height
        // 1.8 Players once again ruin my clean switch-case
        if (this.getClientVersion().isOlderThan(ClientVersion.V_1_9)) {
            return this.isSneaking ? this.possibleEyeHeights[1] : this.possibleEyeHeights[0];
        } else {
            // 1.8 players just have their pose set to standing all the time
            return switch (pose) {
                case FALL_FLYING, // Elytra gliding
                     SPIN_ATTACK, // Riptide trident
                     SWIMMING -> // Swimming (includes crawling in 1.14+)
                        this.possibleEyeHeights[2]; // [swimming/gliding/riptide height, standing height, sneaking height]
                case NINE_CROUCHING, CROUCHING -> this.possibleEyeHeights[1]; // [sneaking height, standing height, swimming/gliding/riptide height]
                default -> this.possibleEyeHeights[0]; // [standing height, sneaking height, swimming/gliding/riptide height]
            };
        }
    }

    @Override
    public int getTransactionPing() {
        return GrimMath.floor(transactionPing / 1e6);
    }

    @Override
    public int getKeepAlivePing() {
        if (bukkitPlayer == null) return -1;
        return PacketEvents.getAPI().getPlayerManager().getPing(bukkitPlayer);
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
        return inVehicle()
                || Collections.max(uncertaintyHandler.pistonX) != 0 || Collections.max(uncertaintyHandler.pistonY) != 0
                || Collections.max(uncertaintyHandler.pistonZ) != 0 || uncertaintyHandler.isStepMovement
                || isFlying || compensatedEntities.self.isDead || isInBed || lastInBed || uncertaintyHandler.lastFlyingStatusChange.hasOccurredSince(30)
                || uncertaintyHandler.lastHardCollidingLerpingEntity.hasOccurredSince(3) || uncertaintyHandler.isOrWasNearGlitchyBlock;
    }

    public void handleMountVehicle(int vehicleID) {
        compensatedEntities.serverPlayerVehicle = vehicleID;
        TrackerData data = compensatedEntities.getTrackedEntity(vehicleID);

        if (data != null) {
            // If we actually need to check vehicle movement
            if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_9) && getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9)) {
                // And if the vehicle is a type of vehicle that we track
                if (EntityTypes.isTypeInstanceOf(data.getEntityType(), EntityTypes.BOAT) || EntityTypes.isTypeInstanceOf(data.getEntityType(), EntityTypes.ABSTRACT_HORSE) || data.getEntityType() == EntityTypes.PIG || data.getEntityType() == EntityTypes.STRIDER) {
                    // We need to set its velocity otherwise it will jump a bit on us, flagging the anticheat
                    // The server does override this with some vehicles. This is intentional.
                    user.writePacket(new WrapperPlayServerEntityVelocity(vehicleID, new Vector3d()));
                }
            }
        }

        // Help prevent transaction split
        sendTransaction();

        latencyUtils.addRealTimeTask(lastTransactionSent.get(), () -> this.vehicleData.wasVehicleSwitch = true);
    }

    public int getRidingVehicleId() {
        return compensatedEntities.getPacketEntityID(compensatedEntities.self.getRiding());
    }

    public void handleDismountVehicle(PacketSendEvent event) {
        // Help prevent transaction split
        sendTransaction();

        compensatedEntities.serverPlayerVehicle = null;
        event.getTasksAfterSend().add(() -> {
            if (inVehicle()) {
                int ridingId = getRidingVehicleId();
                TrackerData data = compensatedEntities.serverPositionsMap.get(ridingId);
                if (data != null) {
                    user.writePacket(new WrapperPlayServerEntityTeleport(ridingId, new Vector3d(data.getX(), data.getY(), data.getZ()), data.getXRot(), data.getYRot(), false));
                }
            }
        });

        latencyUtils.addRealTimeTask(lastTransactionSent.get(), () -> {
            this.vehicleData.wasVehicleSwitch = true;
            // Pre-1.14 players desync sprinting attribute when in vehicle to be false, sprinting itself doesn't change
            if (getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_14)) {
                compensatedEntities.hasSprintingAttributeEnabled = false;
            }
        });
    }

    public boolean canGlide() {
        // Servers older than 1.21.2 don't have this component
        if (getClientVersion().isOlderThan(ClientVersion.V_1_21_2)
                || PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_21_2)) {
            final ItemStack chestPlate = getInventory().getChestplate();
            return chestPlate.getType() == ItemTypes.ELYTRA && chestPlate.getDamageValue() < chestPlate.getMaxDamage();
        }

        final CompensatedInventory inventory = getInventory();
        // PacketEvents mappings are wrong
        // TODO https://github.com/retrooper/packetevents/pull/1125
        return isGlider(inventory.getHelmet(), EquipmentSlot.CHEST_PLATE)
                || isGlider(inventory.getChestplate(), EquipmentSlot.LEGGINGS)
                || isGlider(inventory.getLeggings(), EquipmentSlot.BOOTS)
                || isGlider(inventory.getBoots(), EquipmentSlot.OFF_HAND);
    }

    private static boolean isGlider(ItemStack stack, EquipmentSlot slot) {
        if (!stack.hasComponent(ComponentTypes.GLIDER) || stack.getDamageValue() >= stack.getMaxDamage()) {
            return false;
        }

        Optional<ItemEquippable> equippable = stack.getComponent(ComponentTypes.EQUIPPABLE);
        return equippable.isPresent() && equippable.get().getSlot() == slot;
    }

    public void resyncPose() {
        if (getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_14) && bukkitPlayer != null) {
            bukkitPlayer.setSneaking(!bukkitPlayer.isSneaking());
        }
    }

    public boolean canUseGameMasterBlocks() {
        // This check was added in 1.11
        // 1.11+ players must be in creative and have a permission level at or above 2
        return getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_10) || (gamemode == GameMode.CREATIVE && compensatedEntities.self.getOpLevel() >= 2);
    }

    @Contract(pure = true)
    public boolean supportsEndTick() {
        // TODO: Bypass viaversion
        return getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21_2) && PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_21_2);
    }

    @Contract(pure = true)
    public boolean canSkipTicks() {
        return getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9) && !supportsEndTick();
    }

    @Override
    public void runSafely(Runnable runnable) {
        ChannelHelper.runInEventLoop(this.user.getChannel(), runnable);
    }

    @Override
    public int getLastTransactionReceived() {
        return lastTransactionReceived.get();
    }

    @Override
    public int getLastTransactionSent() {
        return lastTransactionSent.get();
    }

    @Override
    public void addRealTimeTask(int transaction, Runnable runnable) {
        latencyUtils.addRealTimeTask(transaction, runnable);
    }

    @Override
    public String getName() {
        return user.getName();
    }

    @Override
    public UUID getUniqueId() {
        return user.getProfile().getUUID();
    }

    @Override
    public String getBrand() {
        return checkManager.getPacketCheck(ClientBrand.class).getBrand();
    }

    @Override
    public @Nullable String getWorldName() {
        return bukkitPlayer != null ? bukkitPlayer.getWorld().getName() : null;
    }

    @Override
    public @Nullable UUID getWorldUID() {
        return bukkitPlayer != null ? bukkitPlayer.getWorld().getUID() : null;
    }

    @Override
    public String getVersionName() {
        return getClientVersion().getReleaseName();
    }

    @Override
    public double getHorizontalSensitivity() {
        return checkManager.getRotationCheck(AimProcessor.class).sensitivityX;
    }

    @Override
    public double getVerticalSensitivity() {
        return checkManager.getRotationCheck(AimProcessor.class).sensitivityY;
    }

    @Override
    public boolean isVanillaMath() {
        return trigHandler.isVanillaMath();
    }

    @Override
    public Collection<? extends AbstractCheck> getChecks() {
        return checkManager.allChecks.values();
    }

    public void runNettyTaskInMs(Runnable runnable, int ms) {
        Channel channel = (Channel) user.getChannel();
        channel.eventLoop().schedule(runnable, ms, TimeUnit.MILLISECONDS);
    }

    private int maxTransactionTime = 60;
    @Getter private boolean ignoreDuplicatePacketRotation = false;
    @Getter @Setter private boolean experimentalChecks = false;
    @Getter private boolean cancelDuplicatePacket = true;
    @Getter @Setter private boolean exemptElytra = false;
    @Getter private boolean mitigateAutoblock;
    @Getter private boolean mitigateDesyncNoSlow;

    @Override
    public void reload(ConfigManager config) {
        featureManager.onReload(config);
        debugPacketCancel = config.getBooleanElse("debug-packet-cancel", false);
        spamThreshold = config.getIntElse("packet-spam-threshold", 100);
        maxTransactionTime = GrimMath.clamp(config.getIntElse("max-transaction-time", 60), 1, 180);
        ignoreDuplicatePacketRotation = config.getBooleanElse("ignore-duplicate-packet-rotation", false);
        cancelDuplicatePacket = config.getBooleanElse("cancel-duplicate-packet", true);
        mitigateAutoblock = config.getBooleanElse("mitigate-autoblock", true);
        mitigateDesyncNoSlow = config.getBooleanElse("mitigate-desync-noslow", true);
        // reload all checks
        for (AbstractCheck value : checkManager.allChecks.values()) value.reload();
        // reload punishment manager
        punishmentManager.reload(config);
    }

    @Override
    public void reload() {
        reload(GrimAPI.INSTANCE.getConfigManager().getConfig());
    }

    private final FeatureManagerImpl featureManager = new FeatureManagerImpl(this);

    @Override
    public FeatureManager getFeatureManager() {
        return featureManager;
    }

    @Override
    public void sendMessage(String message) {
        if (bukkitPlayer != null) bukkitPlayer.sendMessage(message);
    }

    private ResyncHandler resyncHandler = new BukkitResyncHandler(this);

    @Override
    public ResyncHandler getResyncHandler() {
        return resyncHandler;
    }

    @Override
    public void setResyncHandler(ResyncHandler resyncHandler) {
        this.resyncHandler = resyncHandler;
    }

}
