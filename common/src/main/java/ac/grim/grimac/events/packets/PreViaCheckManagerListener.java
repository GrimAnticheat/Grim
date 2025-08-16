package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.*;
import ac.grim.grimac.utils.change.BlockModification;
import ac.grim.grimac.utils.data.HeadRotation;
import ac.grim.grimac.utils.data.RotationData;
import ac.grim.grimac.utils.data.TeleportAcceptData;
import ac.grim.grimac.utils.data.VelocityData;
import ac.grim.grimac.utils.latency.CompensatedWorld;
import ac.grim.grimac.utils.math.VectorUtils;
import ac.grim.grimac.utils.nmsutil.BlockBreakSpeed;
import ac.grim.grimac.utils.nmsutil.Collisions;
import ac.grim.grimac.utils.nmsutil.Materials;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientVehicleMove;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class PreViaCheckManagerListener extends PacketListenerAbstract {
    // Manual filter on FINISH_DIGGING to prevent clients setting non-breakable blocks to air
    private static final Function<StateType, Boolean> BREAKABLE = type -> !type.isAir() && type.getHardness() != -1.0f && type != StateTypes.WATER && type != StateTypes.LAVA;

    public PreViaCheckManagerListener() {
        super(PacketListenerPriority.LOW);
    }

    @Override
    public boolean isPreVia() {
        return true;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
        if (player == null) return;

        // Determine if teleport BEFORE we call the pre-prediction vehicle
        if (event.getPacketType() == PacketType.Play.Client.VEHICLE_MOVE) {
            WrapperPlayClientVehicleMove move = new WrapperPlayClientVehicleMove(event);
            Vector3d position = move.getPosition();
            player.packetStateData.lastPacketWasTeleport = player.getSetbackTeleportUtil().checkVehicleTeleportQueue(position.getX(), position.getY(), position.getZ());
        }

        TeleportAcceptData teleportData = null;

        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            player.serverOpenedInventoryThisTick = false;

            WrapperPlayClientPlayerFlying flying = new WrapperPlayClientPlayerFlying(event);

            Vector3d position = VectorUtils.clampVector(flying.getLocation().getPosition());
            // Teleports must be POS LOOK
            teleportData = flying.hasPositionChanged() && flying.hasRotationChanged() ? player.getSetbackTeleportUtil().checkTeleportQueue(position.getX(), position.getY(), position.getZ()) : new TeleportAcceptData();
            player.packetStateData.lastPacketWasTeleport = teleportData.isTeleport();

            if (flying.hasRotationChanged() && !flying.hasPositionChanged() && !flying.isOnGround() && !flying.isHorizontalCollision()) {
                List<RotationData> rotations = new ArrayList<>();

                for (RotationData data : player.pendingRotations) {
                    rotations.add(data);
                    if (!data.isAccepted()) {
                        break;
                    }
                }

                // reverse to handle the unaccepted possibility first
                Collections.reverse(rotations);

                for (RotationData data : rotations) {
                    if (data.getYaw() == flying.getLocation().getYaw() && data.getPitch() == flying.getLocation().getPitch() && data.getTransaction() == player.getLastTransactionReceived()) {
                        player.packetStateData.lastPacketWasTeleport = true;
                        data.accept(); // we could be wrong (especially in vehicles), don't remove this
                        break;
                    }
                }
            }

            player.packetStateData.lastPacketWasOnePointSeventeenDuplicate = isMojangStupid(player, event, flying);
        }

        if (player.inVehicle() ? event.getPacketType() == PacketType.Play.Client.VEHICLE_MOVE : WrapperPlayClientPlayerFlying.isFlying(event.getPacketType()) && !player.packetStateData.lastPacketWasOnePointSeventeenDuplicate) {
            // Update knockback and explosions immediately, before anything can setback
            int kbEntityId = player.inVehicle() ? player.getRidingVehicleId() : player.entityID;

            VelocityData calculatedFirstBreadKb = player.checkManager.getKnockbackHandler().calculateFirstBreadKnockback(kbEntityId, player.lastTransactionReceived.get());
            VelocityData calculatedRequireKb = player.checkManager.getKnockbackHandler().calculateRequiredKB(kbEntityId, player.lastTransactionReceived.get(), false);
            player.firstBreadKB = calculatedFirstBreadKb == null ? player.firstBreadKB : calculatedFirstBreadKb;
            player.likelyKB = calculatedRequireKb == null ? player.likelyKB : calculatedRequireKb;

            VelocityData calculateFirstBreadExplosion = player.checkManager.getExplosionHandler().getFirstBreadAddedExplosion(player.lastTransactionReceived.get());
            VelocityData calculateRequiredExplosion = player.checkManager.getExplosionHandler().getPossibleExplosions(player.lastTransactionReceived.get(), false);
            player.firstBreadExplosion = calculateFirstBreadExplosion == null ? player.firstBreadExplosion : calculateFirstBreadExplosion;
            player.likelyExplosions = calculateRequiredExplosion == null ? player.likelyExplosions : calculateRequiredExplosion;
        }

        player.checkManager.onPrePredictionReceivePacket(event);

        // The player flagged crasher or timer checks, therefore we must protect predictions against these attacks
        if (event.isCancelled() && (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType()) || event.getPacketType() == PacketType.Play.Client.VEHICLE_MOVE)) {
            player.packetStateData.cancelDuplicatePacket = false;
            return;
        }

        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            WrapperPlayClientPlayerFlying flying = new WrapperPlayClientPlayerFlying(event);
            Location pos = flying.getLocation();
            boolean ignoreRotation = player.packetStateData.lastPacketWasOnePointSeventeenDuplicate && player.isIgnoreDuplicatePacketRotation();
            handleFlying(player, pos.getX(), pos.getY(), pos.getZ(), ignoreRotation ? 0 : pos.getYaw(), ignoreRotation ? 0 : pos.getPitch(), flying.hasPositionChanged(), flying.hasRotationChanged() && !ignoreRotation, flying.isOnGround(), teleportData, event);
        }

        if (event.getPacketType() == PacketType.Play.Client.VEHICLE_MOVE && player.inVehicle()) {
            WrapperPlayClientVehicleMove move = new WrapperPlayClientVehicleMove(event);
            Vector3d position = move.getPosition();

            player.lastX = player.x;
            player.lastY = player.y;
            player.lastZ = player.z;

            Vector3d clamp = VectorUtils.clampVector(position);
            player.x = clamp.getX();
            player.y = clamp.getY();
            player.z = clamp.getZ();

            player.xRot = move.getYaw();
            player.yRot = move.getPitch();

            final VehiclePositionUpdate update = new VehiclePositionUpdate(clamp, position, move.getYaw(), move.getPitch(), player.packetStateData.lastPacketWasTeleport);
            player.checkManager.onVehiclePositionUpdate(update);

            player.packetStateData.receivedSteerVehicle = false;
        }

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            handleDigging(player, event);
        }

        player.checkManager.onPreViaPacketReceive(event);

        if (event.getPacketType() == PacketType.Play.Client.CLIENT_TICK_END) {
            player.serverOpenedInventoryThisTick = false;
            if (!player.packetStateData.didSendMovementBeforeTickEnd) {
                // The player didn't send a movement packet, so we can predict this like we had idle tick on 1.8
                player.packetStateData.didLastLastMovementIncludePosition = player.packetStateData.didLastMovementIncludePosition;
                player.packetStateData.didLastMovementIncludePosition = false;
            }
            player.packetStateData.didSendMovementBeforeTickEnd = false;
        }

        if (event.isCancelled()) { // will not reach CheckManagerListener
            player.packetStateData.lastPacketWasOnePointSeventeenDuplicate = false;
            player.packetStateData.lastPacketWasTeleport = false;
        }
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
        if (player == null) return;

        player.checkManager.onPreViaPacketSend(event);
    }

    private boolean isMojangStupid(GrimPlayer player, PacketReceiveEvent event, WrapperPlayClientPlayerFlying flying) {
        // Teleports are not stupidity packets.
        if (player.packetStateData.lastPacketWasTeleport) return false;
        // Mojang has become less stupid!
        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21)) return false;

        final Location location = flying.getLocation();
        final double threshold = player.getMovementThreshold();

        // Don't check duplicate 1.17 packets (Why would you do this mojang?)
        // Don't check rotation since it changes between these packets, with the second being irrelevant.
        //
        // removed a large rant, but I'm keeping this out of context insult below
        // EVEN A BUNCH OF MONKEYS ON A TYPEWRITER COULDNT WRITE WORSE NETCODE THAN MOJANG
        if (!player.packetStateData.lastPacketWasTeleport && flying.hasPositionChanged() && flying.hasRotationChanged() &&
                // Ground status will never change in this stupidity packet
                ((flying.isOnGround() == player.packetStateData.packetPlayerOnGround
                        // Mojang added this stupid mechanic in 1.17
                        && (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_17) &&
                        // Due to 0.03, we can't check exact position, only within 0.03
                        player.filterMojangStupidityOnMojangStupidity.distanceSquared(location.getPosition()) < threshold * threshold))
                        // If the player was in a vehicle, has position and look, and wasn't a teleport, then it was this stupid packet
                        || player.inVehicle())) {

            // Mark that we want this packet to be cancelled from reaching the server
            // Additionally, only yaw/pitch matters: https://github.com/GrimAnticheat/Grim/issues/1275#issuecomment-1872444018
            // 1.9+ isn't impacted by this packet as much.
            if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_9)) {
                if (player.isCancelDuplicatePacket()) {
                    player.packetStateData.cancelDuplicatePacket = true;
                }
            } else {
                // Override location to force it to use the last real position of the player. Prevents position-related bypasses like nofall.
                flying.setLocation(new Location(player.filterMojangStupidityOnMojangStupidity.getX(), player.filterMojangStupidityOnMojangStupidity.getY(), player.filterMojangStupidityOnMojangStupidity.getZ(), location.getYaw(), location.getPitch()));
                event.markForReEncode(true);
            }

            player.packetStateData.lastPacketWasOnePointSeventeenDuplicate = true;

            if (!player.isIgnoreDuplicatePacketRotation()) {
                if (player.xRot != location.getYaw() || player.yRot != location.getPitch()) {
                    player.lastXRot = player.xRot;
                    player.lastYRot = player.yRot;
                }

                // Take the pitch and yaw, just in case we were wrong about this being a stupidity packet
                player.xRot = location.getYaw();
                player.yRot = location.getPitch();
            }

            player.packetStateData.lastClaimedPosition = location.getPosition();
            return true;
        }
        return false;
    }

    private void handleFlying(GrimPlayer player, double x, double y, double z, float yaw, float pitch, boolean hasPosition, boolean hasLook, boolean onGround, TeleportAcceptData teleportData, PacketReceiveEvent event) {
        long now = System.currentTimeMillis();

        if (!hasPosition) {
            // This may need to be secured later, although nothing that is very important relies on this
            // 1.8 ghost clients can't abuse this anyway
            player.uncertaintyHandler.lastPointThree.reset();
        }

        // We can't set the look if this is actually the stupidity packet
        // If the last packet wasn't stupid, then ignore this logic
        // If it was stupid, only change the look if it's different
        // Otherwise, reach and fireworks can false
        if (hasLook && (!player.packetStateData.lastPacketWasOnePointSeventeenDuplicate ||
                player.xRot != yaw || player.yRot != pitch)) {
            player.lastXRot = player.xRot;
            player.lastYRot = player.yRot;
        }

        CheckManagerListener.handleQueuedPlaces(player, hasLook, pitch, yaw, now);
        CheckManagerListener.handleQueuedBreaks(player, hasLook, pitch, yaw, now);

        // We can set the new pos after the places
        if (hasPosition) {
            player.packetStateData.lastClaimedPosition = new Vector3d(x, y, z);
        }

        // This stupid mechanic has been measured with 0.03403409022229198 y velocity... DAMN IT MOJANG, use 0.06 to be safe...
        if (!hasPosition && onGround != player.packetStateData.packetPlayerOnGround && !player.inVehicle()) {
            // Check for blocks within 0.03 of the player's position before allowing ground to be true - if 0.03
            // Cannot use collisions like normal because stepping messes it up :(
            //
            // This may need to be secured better, but limiting the new setback positions seems good enough for now...
            boolean canFeasiblyPointThree = Collisions.slowCouldPointThreeHitGround(player, player.x, player.y, player.z);
            if (!canFeasiblyPointThree && !player.compensatedWorld.isNearHardEntity(player.boundingBox.copy().expand(4))
                    || player.clientVelocity.getY() > 0.06 && !player.uncertaintyHandler.wasAffectedByStuckSpeed()) {
                // Ghost block/0.03 abuse
                player.getSetbackTeleportUtil().executeForceResync();
            } else {
                // Accept the new ground status
                player.lastOnGround = onGround;
                player.clientClaimsLastOnGround = onGround;
                player.uncertaintyHandler.onGroundUncertain = true;
            }
        }

        if (!player.packetStateData.lastPacketWasTeleport) {
            player.packetStateData.packetPlayerOnGround = onGround;
        }

        if (hasLook) {
            player.xRot = yaw;
            player.yRot = pitch;

            float deltaXRot = player.xRot - player.lastXRot;
            float deltaYRot = player.yRot - player.lastYRot;

            final RotationUpdate update = new RotationUpdate(new HeadRotation(player.lastXRot, player.lastYRot), new HeadRotation(player.xRot, player.yRot), deltaXRot, deltaYRot);
            player.checkManager.onRotationUpdate(update);
        }

        if (hasPosition) {
            Vector3d position = new Vector3d(x, y, z);
            Vector3d clampVector = VectorUtils.clampVector(position);
            final PositionUpdate update = new PositionUpdate(new Vector3d(player.x, player.y, player.z), position, onGround, teleportData.getSetback(), teleportData.getTeleportData(), teleportData.isTeleport());

            // Stupidity doesn't care about 0.03
            if (!player.packetStateData.lastPacketWasOnePointSeventeenDuplicate) {
                player.filterMojangStupidityOnMojangStupidity = clampVector;
            }

            if (!player.inVehicle() && !player.packetStateData.lastPacketWasOnePointSeventeenDuplicate) {
                player.lastX = player.x;
                player.lastY = player.y;
                player.lastZ = player.z;

                player.x = clampVector.getX();
                player.y = clampVector.getY();
                player.z = clampVector.getZ();

                player.checkManager.onPositionUpdate(update);
            } else if (update.isTeleport()) { // Mojang doesn't use their own exit vehicle field to leave vehicles, manually call the setback handler
                player.getSetbackTeleportUtil().onPredictionComplete(new PredictionComplete(0, update, true));
            }
        }

        player.packetStateData.didLastLastMovementIncludePosition = player.packetStateData.didLastMovementIncludePosition;
        player.packetStateData.didLastMovementIncludePosition = hasPosition;

        if (!player.packetStateData.lastPacketWasTeleport) {
            player.packetStateData.didSendMovementBeforeTickEnd = true;
        }

        player.packetStateData.horseInteractCausedForcedRotation = false;
    }

    private void handleDigging(GrimPlayer player, PacketReceiveEvent event) {
        player.lastBlockBreak = System.currentTimeMillis();

        final WrapperPlayClientPlayerDigging packet = new WrapperPlayClientPlayerDigging(event);
        final DiggingAction action = packet.getAction();

        if (action != DiggingAction.START_DIGGING
                && action != DiggingAction.FINISHED_DIGGING
                && action != DiggingAction.CANCELLED_DIGGING) {
            return;
        }

        final BlockBreak blockBreak = new BlockBreak(player, packet.getBlockPosition(), packet.getBlockFace(), packet.getBlockFaceId(), action, packet.getSequence(), player.compensatedWorld.getBlock(packet.getBlockPosition()));

        player.checkManager.onBlockBreak(blockBreak);

        if (blockBreak.isCancelled()) {
            event.setCancelled(true);
            player.onPacketCancel();
            player.resyncPosition(blockBreak.position, packet.getSequence());
            return;
        }

        player.queuedBreaks.add(blockBreak);

        if (action == DiggingAction.FINISHED_DIGGING && BREAKABLE.apply(blockBreak.block.getType())) {
            player.compensatedWorld.startPredicting();
            player.compensatedWorld.updateBlock(blockBreak.position.x, blockBreak.position.y, blockBreak.position.z, 0);
            player.compensatedWorld.stopPredicting(packet);
        }

        if (action == DiggingAction.START_DIGGING) {
            double damage = BlockBreakSpeed.getBlockDamage(player, blockBreak.block);

            // Instant breaking, no damage means it is unbreakable by creative players (with swords)
            if (damage >= 1) {
                player.compensatedWorld.startPredicting();
                player.blockHistory.add(
                        new BlockModification(
                                player.compensatedWorld.getBlock(blockBreak.position),
                                WrappedBlockState.getByGlobalId(0),
                                blockBreak.position,
                                GrimAPI.INSTANCE.getTickManager().currentTick,
                                BlockModification.Cause.START_DIGGING
                        )
                );
                if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_13) && Materials.isWaterSource(player.getClientVersion(), blockBreak.block)) {
                    // Vanilla uses a method to grab water flowing, but as you can't break flowing water
                    // We can simply treat all waterlogged blocks or source blocks as source blocks
                    player.compensatedWorld.updateBlock(blockBreak.position, StateTypes.WATER.createBlockState(CompensatedWorld.blockVersion));
                } else {
                    player.compensatedWorld.updateBlock(blockBreak.position.x, blockBreak.position.y, blockBreak.position.z, 0);
                }
                player.compensatedWorld.stopPredicting(packet);
            }
        }

        player.compensatedWorld.handleBlockBreakPrediction(packet);
    }
}
