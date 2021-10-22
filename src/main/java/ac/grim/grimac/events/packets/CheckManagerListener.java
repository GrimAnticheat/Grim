package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockPlace;
import ac.grim.grimac.utils.anticheat.update.PositionUpdate;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;
import ac.grim.grimac.utils.anticheat.update.VehiclePositionUpdate;
import ac.grim.grimac.utils.blockplace.BlockPlaceResult;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.TeleportAcceptData;
import ac.grim.grimac.utils.math.VectorUtils;
import ac.grim.grimac.utils.nmsImplementations.GetBoundingBox;
import io.github.retrooper.packetevents.event.PacketListenerAbstract;
import io.github.retrooper.packetevents.event.PacketListenerPriority;
import io.github.retrooper.packetevents.event.impl.PacketPlayReceiveEvent;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.play.in.blockplace.WrappedPacketInBlockPlace;
import io.github.retrooper.packetevents.packetwrappers.play.in.flying.WrappedPacketInFlying;
import io.github.retrooper.packetevents.packetwrappers.play.in.vehiclemove.WrappedPacketInVehicleMove;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import io.github.retrooper.packetevents.utils.player.Direction;
import io.github.retrooper.packetevents.utils.server.ServerVersion;
import io.github.retrooper.packetevents.utils.vector.Vector3d;
import io.github.retrooper.packetevents.utils.vector.Vector3i;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class CheckManagerListener extends PacketListenerAbstract {

    long lastPosLook = 0;

    public CheckManagerListener() {
        super(PacketListenerPriority.LOW);
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
            if (hasPosition && hasLook && !player.packetStateData.lastPacketWasTeleport) {
                if ((player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_17) &&
                        player.packetStateData.packetPosition.equals(flying.getPosition())) || player.packetStateData.isInVehicle) {
                    lastPosLook = System.currentTimeMillis();
                    player.packetStateData.lastPacketWasOnePointSeventeenDuplicate = true;

                    // Don't let players on 1.17+ clients on 1.8- servers FastHeal by right-clicking
                    // the ground with a bucket... ViaVersion marked this as a WONTFIX, so I'll include the fix.
                    if (ServerVersion.getVersion().isOlderThanOrEquals(ServerVersion.v_1_8_8)) {
                        event.setCancelled(true);
                    }
                    return;
                }
            }

            lastPosLook = System.currentTimeMillis();

            if (!hasPosition && onGround != player.packetStateData.packetPlayerOnGround)
                player.packetStateData.didGroundStatusChangeWithoutPositionPacket = true;

            player.packetStateData.lastPacketPlayerXRot = player.packetStateData.packetPlayerXRot;
            player.packetStateData.lastPacketPlayerYRot = player.packetStateData.packetPlayerYRot;
            player.packetStateData.lastPacketPosition = player.packetStateData.packetPosition;
            player.packetStateData.lastPacketWasOnePointSeventeenDuplicate = false;

            // Go test with a 1.8 client on a 1.17 server, and you will see
            player.packetStateData.packetPlayerOnGround = onGround;

            if (hasLook) {
                float xRot = flying.getYaw();
                float yRot = flying.getPitch();

                player.packetStateData.packetPlayerXRot = xRot;
                player.packetStateData.packetPlayerYRot = yRot;
            }

            if (hasPosition) {
                Vector3d position = flying.getPosition();
                player.packetStateData.packetPosition = VectorUtils.clampVector(position);

                final PositionUpdate update = new PositionUpdate(player.packetStateData.lastPacketPosition, position, onGround, teleportData.isTeleport(), teleportData.getSetback());
                player.checkManager.onPositionUpdate(update);
            }

            if (hasLook) {
                float xRot = flying.getYaw();
                float yRot = flying.getPitch();

                float deltaXRot = xRot - player.packetStateData.lastPacketPlayerXRot;
                float deltaYRot = yRot - player.packetStateData.lastPacketPlayerYRot;

                final RotationUpdate update = new RotationUpdate(player.packetStateData.lastPacketPlayerXRot, player.packetStateData.lastPacketPlayerYRot, xRot, yRot, deltaXRot, deltaYRot);
                player.checkManager.onRotationUpdate(update);
            }

            player.packetStateData.didLastLastMovementIncludePosition = player.packetStateData.didLastMovementIncludePosition;
            player.packetStateData.didLastMovementIncludePosition = hasPosition;
            player.packetStateData.movementPacketsReceived++;
        }

        if (packetID == PacketType.Play.Client.VEHICLE_MOVE) {
            WrappedPacketInVehicleMove move = new WrappedPacketInVehicleMove(event.getNMSPacket());
            Vector3d position = move.getPosition();

            player.packetStateData.lastPacketPosition = player.packetStateData.packetPosition;
            player.packetStateData.packetPosition = VectorUtils.clampVector(position);

            final boolean isTeleport = player.getSetbackTeleportUtil().checkVehicleTeleportQueue(position.getX(), position.getY(), position.getZ());
            player.packetStateData.lastPacketWasTeleport = isTeleport;
            final VehiclePositionUpdate update = new VehiclePositionUpdate(player.packetStateData.packetPosition, position, move.getYaw(), move.getPitch(), isTeleport);
            player.checkManager.onVehiclePositionUpdate(update);

            player.packetStateData.receivedSteerVehicle = false;
        }

        if (PacketType.Play.Client.Util.isBlockPlace(event.getPacketId())) {
            WrappedPacketInBlockPlace place = new WrappedPacketInBlockPlace(event.getNMSPacket());
            Vector3i blockPosition = place.getBlockPosition();
            Direction face = place.getDirection();

            ItemStack placedWith = player.bukkitPlayer.getInventory().getItem(player.packetStateData.lastSlotSelected);

            // I swear if Bukkit doesn't do .isBlock() accurately...
            if (placedWith != null) {
                Material material = transformMaterial(placedWith);
                if (!material.isBlock()) return;

                BlockPlace blockPlace = new BlockPlace(player, blockPosition, face, material);

                player.checkManager.onBlockPlace(blockPlace);

                if (!blockPlace.isCancelled()) {

                    int blockX = blockPlace.getPlacedBlockPos().getX();
                    int blockY = blockPlace.getPlacedBlockPos().getY();
                    int blockZ = blockPlace.getPlacedBlockPos().getZ();

                    double playerX = player.packetStateData.packetPosition.getX();
                    double playerY = player.packetStateData.packetPosition.getY();
                    double playerZ = player.packetStateData.packetPosition.getZ();

                    // Hard coded as stone as proof of concept
                    SimpleCollisionBox playerBox = GetBoundingBox.getBoundingBoxFromPosAndSize(playerX, playerY, playerZ, 0.6, 1.8);

                    // isIntersected != isCollided.  Intersection means check overlap, collided also checks if equal
                    // CollisionData.getData(type).getMovementCollisionBox(player, player.getClientVersion(), magicData, placed.getX(), placed.getY(), placed.getZ()
                    // The block was not placed inside the player and therefore the place should be processed by block place result to check if it's successful
                    BlockPlaceResult.getMaterialData(material).applyBlockPlaceToWorld(player, blockPlace);
                }
            }
        }

        // Call the packet checks last as they can modify the contents of the packet
        // Such as the NoFall check setting the player to not be on the ground
        player.checkManager.onPacketReceive(event);
    }

    // For example, placing seeds to place wheat
    // TODO: Make this compatible with previous versions by using XMaterial
    private Material transformMaterial(ItemStack stack) {
        if (stack.getType() == Material.COCOA_BEANS) return Material.COCOA;
        if (stack.getType() == Material.INK_SAC && stack.getDurability() == 3) return Material.COCOA;
        if (stack.getType() == Material.FIRE_CHARGE) return Material.FIRE;
        if (stack.getType() == Material.POTATO) return Material.POTATOES;
        if (stack.getType() == Material.BEETROOT_SEEDS) return Material.BEETROOTS;
        if (stack.getType() == Material.CARROT) return Material.CARROTS;
        if (stack.getType() == Material.PUMPKIN_SEEDS) return Material.PUMPKIN_STEM;
        if (stack.getType() == Material.MELON_STEM) return Material.MELON_STEM;
        if (stack.getType() == Material.WHEAT_SEEDS) return Material.WHEAT;
        if (stack.getType() == Material.REDSTONE) return Material.REDSTONE_WIRE;

        return stack.getType();
    }

    @Override
    public void onPacketPlaySend(PacketPlaySendEvent event) {
        GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
        if (player == null) return;

        player.checkManager.onPacketSend(event);
    }
}
