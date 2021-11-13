package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockPlace;
import ac.grim.grimac.utils.anticheat.update.PositionUpdate;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;
import ac.grim.grimac.utils.anticheat.update.VehiclePositionUpdate;
import ac.grim.grimac.utils.blockplace.BlockPlaceResult;
import ac.grim.grimac.utils.blockstate.BaseBlockState;
import ac.grim.grimac.utils.blockstate.helper.BlockStateHelper;
import ac.grim.grimac.utils.collisions.HitboxData;
import ac.grim.grimac.utils.collisions.datatypes.CollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.HitData;
import ac.grim.grimac.utils.data.TeleportAcceptData;
import ac.grim.grimac.utils.math.GrimMath;
import ac.grim.grimac.utils.math.VectorUtils;
import ac.grim.grimac.utils.nmsutil.*;
import io.github.retrooper.packetevents.event.PacketListenerAbstract;
import io.github.retrooper.packetevents.event.PacketListenerPriority;
import io.github.retrooper.packetevents.event.impl.PacketPlayReceiveEvent;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.play.in.blockplace.WrappedPacketInBlockPlace;
import io.github.retrooper.packetevents.packetwrappers.play.in.flying.WrappedPacketInFlying;
import io.github.retrooper.packetevents.packetwrappers.play.in.vehiclemove.WrappedPacketInVehicleMove;
import io.github.retrooper.packetevents.utils.pair.Pair;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import io.github.retrooper.packetevents.utils.player.Direction;
import io.github.retrooper.packetevents.utils.server.ServerVersion;
import io.github.retrooper.packetevents.utils.vector.Vector3d;
import io.github.retrooper.packetevents.utils.vector.Vector3i;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public class CheckManagerListener extends PacketListenerAbstract {

    long lastPosLook = 0;

    public CheckManagerListener() {
        super(PacketListenerPriority.LOW);
    }

    // Copied from MCP...
    // Returns null if there isn't anything.
    //
    // I do have to admit that I'm starting to like bifunctions/new java 8 things more than I originally did.
    // although I still don't understand Mojang's obsession with streams in some of the hottest methods... that kills performance
    static HitData traverseBlocks(GrimPlayer player, Vector3d start, Vector3d end, BiFunction<BaseBlockState, Vector3i, HitData> predicate) {
        // I guess go back by the collision epsilon?
        double endX = GrimMath.lerp(-1.0E-7D, end.x, start.x);
        double endY = GrimMath.lerp(-1.0E-7D, end.y, start.y);
        double endZ = GrimMath.lerp(-1.0E-7D, end.z, start.z);
        double startX = GrimMath.lerp(-1.0E-7D, start.x, end.x);
        double startY = GrimMath.lerp(-1.0E-7D, start.y, end.y);
        double startZ = GrimMath.lerp(-1.0E-7D, start.z, end.z);
        int floorStartX = GrimMath.floor(startX);
        int floorStartY = GrimMath.floor(startY);
        int floorStartZ = GrimMath.floor(startZ);


        if (start.equals(end)) return null;

        BaseBlockState state = player.compensatedWorld.getWrappedBlockStateAt(floorStartX, floorStartY, floorStartZ);
        HitData apply = predicate.apply(state, new Vector3i(floorStartX, floorStartY, floorStartZ));

        if (apply != null) {
            return apply;
        }

        double xDiff = endX - startX;
        double yDiff = endY - startY;
        double zDiff = endZ - startZ;
        int xSign = GrimMath.sign(xDiff);
        int ySign = GrimMath.sign(yDiff);
        int zSign = GrimMath.sign(zDiff);

        double posXInverse = xSign == 0 ? Double.MAX_VALUE : xSign / xDiff;
        double posYInverse = ySign == 0 ? Double.MAX_VALUE : ySign / yDiff;
        double posZInverse = zSign == 0 ? Double.MAX_VALUE : zSign / zDiff;

        double d12 = posXInverse * (xSign > 0 ? 1.0D - GrimMath.frac(startX) : GrimMath.frac(startX));
        double d13 = posYInverse * (ySign > 0 ? 1.0D - GrimMath.frac(startY) : GrimMath.frac(startY));
        double d14 = posZInverse * (zSign > 0 ? 1.0D - GrimMath.frac(startZ) : GrimMath.frac(startZ));

        // Can't figure out what this code does currently
        while (d12 <= 1.0D || d13 <= 1.0D || d14 <= 1.0D) {
            if (d12 < d13) {
                if (d12 < d14) {
                    floorStartX += xSign;
                    d12 += posXInverse;
                } else {
                    floorStartZ += zSign;
                    d14 += posZInverse;
                }
            } else if (d13 < d14) {
                floorStartY += ySign;
                d13 += posYInverse;
            } else {
                floorStartZ += zSign;
                d14 += posZInverse;
            }

            state = player.compensatedWorld.getWrappedBlockStateAt(floorStartX, floorStartY, floorStartZ);
            apply = predicate.apply(state, new Vector3i(floorStartX, floorStartY, floorStartZ));

            if (apply != null) {
                return apply;
            }
        }

        return null;
    }

    @Override
    public void onPacketPlayReceive(PacketPlayReceiveEvent event) {
        byte packetID = event.getPacketId();

        GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
        if (player == null) return;

        if (PacketType.Play.Client.Util.isInstanceOfFlying(packetID)) {
            WrappedPacketInFlying flying = new WrappedPacketInFlying(event.getNMSPacket());

            boolean hasPosition = packetID == PacketType.Play.Client.POSITION || packetID == PacketType.Play.Client.POSITION_LOOK;
            boolean hasLook = packetID == PacketType.Play.Client.LOOK || packetID == PacketType.Play.Client.POSITION_LOOK;
            boolean onGround = flying.isOnGround();

            player.packetStateData.lastPacketWasTeleport = false;
            TeleportAcceptData teleportData = null;
            if (hasPosition) {
                Vector3d position = VectorUtils.clampVector(flying.getPosition());
                teleportData = player.getSetbackTeleportUtil().checkTeleportQueue(position.getX(), position.getY(), position.getZ());
                player.packetStateData.lastPacketWasTeleport = teleportData.isTeleport();
            }

            // Don't check duplicate 1.17 packets (Why would you do this mojang?)
            // Don't check rotation since it changes between these packets, with the second being irrelevant.
            //
            // If a player sends a POS LOOK in a vehicle... then it was this stupid fucking mechanic
            if (hasPosition && hasLook && !player.packetStateData.lastPacketWasTeleport &&
                    (player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_17) &&
                            new Vector3d(player.x, player.y, player.z).equals(flying.getPosition())) || player.inVehicle) {
                lastPosLook = System.currentTimeMillis();
                player.packetStateData.lastPacketWasOnePointSeventeenDuplicate = true;

                // Don't let players on 1.17+ clients on 1.8- servers FastHeal by right-clicking
                // the ground with a bucket... ViaVersion marked this as a WONTFIX, so I'll include the fix.
                if (ServerVersion.getVersion().isOlderThanOrEquals(ServerVersion.v_1_8_8)) {
                    event.setCancelled(true);
                }
                return;
            }

            lastPosLook = System.currentTimeMillis();

            // Check for blocks within 0.03 of the player's position before allowing ground to be true - if 0.03
            boolean nearGround = Collisions.collide(player, 0, -0.03, 0).getY() != -0.03;
            if (!hasPosition && onGround != player.packetStateData.packetPlayerOnGround && nearGround && player.clientVelocity.getY() < 0.03) {
                player.lastOnGround = true;
                player.uncertaintyHandler.onGroundUncertain = true;
                player.uncertaintyHandler.lastTickWasNearGroundZeroPointZeroThree = true;
                player.clientClaimsLastOnGround = true;
            }

            player.lastX = player.x;
            player.lastY = player.y;
            player.lastZ = player.z;
            player.lastXRot = player.xRot;
            player.lastYRot = player.yRot;

            player.packetStateData.lastPacketWasOnePointSeventeenDuplicate = false;

            player.packetStateData.packetPlayerOnGround = onGround;

            if (hasLook) {
                player.xRot = flying.getYaw();
                player.yRot = flying.getPitch();
            }

            if (hasPosition) {
                Vector3d position = flying.getPosition();
                Vector3d clampVector = VectorUtils.clampVector(position);

                player.x = clampVector.getX();
                player.y = clampVector.getY();
                player.z = clampVector.getZ();

                final PositionUpdate update = new PositionUpdate(new Vector3d(player.x, player.y, player.z), position, onGround, teleportData.isTeleport(), teleportData.getSetback());
                player.checkManager.onPositionUpdate(update);
            }

            if (hasLook) {
                float deltaXRot = player.xRot - player.lastXRot;
                float deltaYRot = player.yRot - player.lastYRot;

                final RotationUpdate update = new RotationUpdate(player.lastXRot, player.lastYRot, player.xRot, player.yRot, deltaXRot, deltaYRot);
                player.checkManager.onRotationUpdate(update);
            }

            player.packetStateData.didLastLastMovementIncludePosition = player.packetStateData.didLastMovementIncludePosition;
            player.packetStateData.didLastMovementIncludePosition = hasPosition;
        }

        if (packetID == PacketType.Play.Client.VEHICLE_MOVE) {
            WrappedPacketInVehicleMove move = new WrappedPacketInVehicleMove(event.getNMSPacket());
            Vector3d position = move.getPosition();

            player.lastX = player.x;
            player.lastY = player.y;
            player.lastZ = player.z;

            Vector3d clamp = VectorUtils.clampVector(position);
            player.x = clamp.getX();
            player.y = clamp.getY();
            player.z = clamp.getZ();

            final boolean isTeleport = player.getSetbackTeleportUtil().checkVehicleTeleportQueue(position.getX(), position.getY(), position.getZ());
            player.packetStateData.lastPacketWasTeleport = isTeleport;
            final VehiclePositionUpdate update = new VehiclePositionUpdate(clamp, position, move.getYaw(), move.getPitch(), isTeleport);
            player.checkManager.onVehiclePositionUpdate(update);

            player.packetStateData.receivedSteerVehicle = false;
        }

        // Check for interactable first (door, etc)
        // TODO: Buttons and other interactables (they would block the player from placing another block)
        if (PacketType.Play.Client.Util.isBlockPlace(event.getPacketId()) && !player.isSneaking) {
            WrappedPacketInBlockPlace place = new WrappedPacketInBlockPlace(event.getNMSPacket());
            Vector3i blockPosition = place.getBlockPosition();
            BlockPlace blockPlace = new BlockPlace(player, blockPosition, null, null);

            // Right-clicking a trapdoor/door/etc.
            if (Materials.checkFlag(blockPlace.getPlacedAgainstMaterial(), Materials.CLIENT_SIDE_INTERACTABLE)) {
                Vector3i location = blockPlace.getPlacedAgainstBlockLocation();
                player.compensatedWorld.tickOpenable(location.getX(), location.getY(), location.getZ());
                return;
            }
        }

        if (packetID == PacketType.Play.Client.BLOCK_PLACE) {
            WrappedPacketInBlockPlace place = new WrappedPacketInBlockPlace(event.getNMSPacket());

            // TODO: Support offhand!
            ItemStack placedWith = player.bukkitPlayer.getInventory().getItem(player.packetStateData.lastSlotSelected);
            Material material = transformMaterial(placedWith);
            BlockPlace blockPlace = new BlockPlace(player, null, null, material);

            // Lilypads are USE_ITEM (THIS CAN DESYNC, WTF MOJANG)
            if (material == XMaterial.LILY_PAD.parseMaterial()) {
                placeLilypad(player, blockPlace); // Pass a block place because lily pads have a hitbox
                return;
            }

            Material toBucketMat = Materials.transformBucketMaterial(material);
            if (toBucketMat != null) {
                placeWaterLavaSnowBucket(player, blockPlace, toBucketMat);
            }

            if (material == Material.BUCKET) {
                placeBucket(player, blockPlace);
            }
        }

        if (PacketType.Play.Client.Util.isBlockPlace(event.getPacketId())) {
            WrappedPacketInBlockPlace place = new WrappedPacketInBlockPlace(event.getNMSPacket());
            Vector3i blockPosition = place.getBlockPosition();
            Direction face = place.getDirection();
            // TODO: Support offhand!
            ItemStack placedWith = player.bukkitPlayer.getInventory().getItem(player.packetStateData.lastSlotSelected);
            Material material = transformMaterial(placedWith);
            BlockPlace blockPlace = new BlockPlace(player, blockPosition, face, material);

            if (placedWith != null && material.isBlock()) {
                player.checkManager.onBlockPlace(blockPlace);

                if (!blockPlace.isCancelled()) {
                    BlockPlaceResult.getMaterialData(material).applyBlockPlaceToWorld(player, blockPlace);
                }
            }
        }

        // Call the packet checks last as they can modify the contents of the packet
        // Such as the NoFall check setting the player to not be on the ground
        player.checkManager.onPacketReceive(event);
    }

    private void placeWaterLavaSnowBucket(GrimPlayer player, BlockPlace blockPlace, Material toPlace) {
        HitData data = getNearestHitResult(player, toPlace, false);
        if (data != null) {
            blockPlace.setBlockPosition(data.getPosition());
            blockPlace.setFace(Direction.valueOf(data.getClosestDirection().name()));

            // If we hit a waterloggable block, then the bucket is directly placed
            // Otherwise, use the face to determine where to place the bucket
            if (Materials.isPlaceableLiquidBucket(blockPlace.getMaterial()) && ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_13)) {
                BlockData existing = blockPlace.getExistingBlockBlockData();
                if (existing instanceof Waterlogged) {
                    Waterlogged waterlogged = (Waterlogged) existing.clone(); // Don't corrupt palette
                    waterlogged.setWaterlogged(true);
                    blockPlace.set(waterlogged);
                    return;
                }
            }

            // Powder snow, lava, and water all behave like placing normal blocks after checking for waterlogging
            blockPlace.set(toPlace);
        }
    }

    private void placeBucket(GrimPlayer player, BlockPlace blockPlace) {
        HitData data = getNearestHitResult(player, null, true);
        if (data != null) {
            if (data.getState().getMaterial() == Material.POWDER_SNOW) {
                blockPlace.set(Material.AIR);
                return;
            }

            // We didn't hit fluid
            if (player.compensatedWorld.getFluidLevelAt(data.getPosition().getX(), data.getPosition().getY(), data.getPosition().getZ()) == 0)
                return;

            blockPlace.setBlockPosition(data.getPosition());
            blockPlace.setFace(Direction.valueOf(data.getClosestDirection().name()));

            if (ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_13)) {
                BlockData existing = blockPlace.getExistingBlockBlockData();
                if (existing instanceof Waterlogged) {
                    Waterlogged waterlogged = (Waterlogged) existing.clone(); // Don't corrupt palette
                    waterlogged.setWaterlogged(false);
                    blockPlace.set(waterlogged);
                    return;
                }
            }

            // Therefore, not waterlogged and is a fluid, and is therefore a source block
            blockPlace.set(Material.AIR);
        }
    }

    private void placeLilypad(GrimPlayer player, BlockPlace blockPlace) {
        HitData data = getNearestHitResult(player, null, true);
        if (data != null) {
            // A lilypad cannot replace a fluid
            if (player.compensatedWorld.getFluidLevelAt(data.getPosition().getX(), data.getPosition().getY() + 1, data.getPosition().getZ()) > 0)
                return;

            blockPlace.setBlockPosition(data.getPosition());
            blockPlace.setFace(Direction.valueOf(data.getClosestDirection().name()));

            // We checked for a full fluid block below here.
            if (player.compensatedWorld.getWaterFluidLevelAt(data.getPosition().getX(), data.getPosition().getY(), data.getPosition().getZ()) > 0
                    || data.getState().getMaterial() == Material.ICE || data.getState().getMaterial() == Material.FROSTED_ICE) {
                Vector3i pos = data.getPosition().clone();
                pos.setY(pos.getY() + 1);

                blockPlace.set(pos, BlockStateHelper.create(blockPlace.getMaterial()));
            }
        }
    }

    // For example, placing seeds to place wheat
    // TODO: Make this compatible with previous versions by using XMaterial
    private Material transformMaterial(ItemStack stack) {
        if (stack == null) return null;
        if (stack.getType() == Material.COCOA_BEANS) return Material.COCOA;
        if (stack.getType() == Material.INK_SAC && stack.getDurability() == 3) return Material.COCOA;
        if (stack.getType() == Material.FIRE_CHARGE) return Material.FIRE;
        if (stack.getType() == Material.POTATO) return Material.POTATOES;
        if (stack.getType() == Material.BEETROOT_SEEDS) return Material.BEETROOTS;
        if (stack.getType() == Material.CARROT) return Material.CARROTS;
        if (stack.getType() == Material.PUMPKIN_SEEDS) return Material.PUMPKIN_STEM;
        if (stack.getType() == Material.MELON_SEEDS) return Material.MELON_STEM;
        if (stack.getType() == Material.WHEAT_SEEDS) return Material.WHEAT;
        if (stack.getType() == Material.REDSTONE) return Material.REDSTONE_WIRE;
        if (stack.getType() == Material.POWDER_SNOW_BUCKET) return Material.POWDER_SNOW;

        return stack.getType();
    }

    private HitData getNearestHitResult(GrimPlayer player, Material heldItem, boolean sourcesHaveHitbox) {
        // TODO: When we do this post-tick (fix desync) switch to lastX
        Vector3d startingPos = new Vector3d(player.x, player.y + player.getEyeHeight(), player.z);
        Vector startingVec = new Vector(startingPos.getX(), startingPos.getY(), startingPos.getZ());
        Ray trace = new Ray(player, startingPos.getX(), startingPos.getY(), startingPos.getZ(), player.xRot, player.yRot);
        Vector endVec = trace.getPointAtDistance(6);
        Vector3d endPos = new Vector3d(endVec.getX(), endVec.getY(), endVec.getZ());

        return traverseBlocks(player, startingPos, endPos, (block, vector3i) -> {
            CollisionBox data = HitboxData.getBlockHitbox(player, heldItem, player.getClientVersion(), block, vector3i.getX(), vector3i.getY(), vector3i.getZ());
            List<SimpleCollisionBox> boxes = new ArrayList<>();
            data.downCast(boxes);

            double bestHitResult = Double.MAX_VALUE;
            Vector bestHitLoc = null;
            BlockFace bestFace = null;

            for (SimpleCollisionBox box : boxes) {
                Pair<Vector, BlockFace> intercept = ReachUtils.calculateIntercept(box, trace.getOrigin(), trace.getPointAtDistance(6));
                if (intercept.getFirst() == null) continue; // No intercept

                Vector hitLoc = intercept.getFirst();

                if (hitLoc.distanceSquared(startingVec) < bestHitResult) {
                    bestHitResult = hitLoc.distanceSquared(startingVec);
                    bestHitLoc = hitLoc;
                    bestFace = intercept.getSecond();
                }
            }
            if (bestHitLoc != null) {
                Bukkit.broadcastMessage(bestFace + " ");
                return new HitData(vector3i, bestHitLoc, bestFace, block);
            }

            if (sourcesHaveHitbox &&
                    (player.compensatedWorld.isWaterSourceBlock(vector3i.getX(), vector3i.getY(), vector3i.getZ())
                            || player.compensatedWorld.getLavaFluidLevelAt(vector3i.getX(), vector3i.getY(), vector3i.getZ()) == (8 / 9f))) {
                double waterHeight = player.compensatedWorld.getFluidLevelAt(vector3i.getX(), vector3i.getY(), vector3i.getZ());
                SimpleCollisionBox box = new SimpleCollisionBox(vector3i.getX(), vector3i.getY(), vector3i.getZ(), vector3i.getX() + 1, vector3i.getY() + waterHeight, vector3i.getZ() + 1);

                Pair<Vector, BlockFace> intercept = ReachUtils.calculateIntercept(box, trace.getOrigin(), trace.getPointAtDistance(6));

                if (intercept.getFirst() != null) {
                    return new HitData(vector3i, intercept.getFirst(), intercept.getSecond(), block);
                }
            }

            return null;
        });
    }

    @Override
    public void onPacketPlaySend(PacketPlaySendEvent event) {
        GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
        if (player == null) return;

        player.checkManager.onPacketSend(event);
    }
}
