package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockPlace;
import ac.grim.grimac.utils.anticheat.update.PositionUpdate;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;
import ac.grim.grimac.utils.anticheat.update.VehiclePositionUpdate;
import ac.grim.grimac.utils.blockplace.BlockPlaceResult;
import ac.grim.grimac.utils.data.TeleportAcceptData;
import ac.grim.grimac.utils.math.VectorUtils;
import ac.grim.grimac.utils.nmsImplementations.Materials;
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

            // TODO: Check for blocks within 0.03 of the player!
            if (!hasPosition && onGround != player.packetStateData.packetPlayerOnGround)
                player.packetStateData.didGroundStatusChangeWithoutPositionPacket = true;

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

        if (PacketType.Play.Client.Util.isBlockPlace(event.getPacketId())) {
            WrappedPacketInBlockPlace place = new WrappedPacketInBlockPlace(event.getNMSPacket());
            Vector3i blockPosition = place.getBlockPosition();
            Direction face = place.getDirection();

            ItemStack placedWith = player.bukkitPlayer.getInventory().getItem(player.packetStateData.lastSlotSelected);
            Material material = transformMaterial(placedWith);
            BlockPlace blockPlace = new BlockPlace(player, blockPosition, face, material);

            // Right-clicking a trapdoor/door/etc.
            if (Materials.checkFlag(blockPlace.getPlacedAgainstMaterial(), Materials.CLIENT_SIDE_INTERACTABLE)) {
                Vector3i location = blockPlace.getPlacedAgainstBlockLocation();
                player.compensatedWorld.tickOpenable(location.getX(), location.getY(), location.getZ());
                return;
            }

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

        return stack.getType();
    }

    @Override
    public void onPacketPlaySend(PacketPlaySendEvent event) {
        GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
        if (player == null) return;

        player.checkManager.onPacketSend(event);
    }
}
