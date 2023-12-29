package ac.grim.grimac.events.packets.patch;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.checks.impl.badpackets.BadPacketsU;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;

// This runs before anything else.
public class EnforceUseItemStupidity extends PacketListenerAbstract {

    public EnforceUseItemStupidity() {
        super(PacketListenerPriority.LOWEST);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
        if (player == null) return;

        // Stupidity packet only exists on 1.17+
        if (player.getClientVersion().isOlderThan(ClientVersion.V_1_17)) return;

        final boolean wasPotentiallyOnePointSeventeenDuplicate = player.packetStateData.lastTeleportWasPotentiallyOnePointSeventeenDuplicate;
        if (isUseItem(event)) {
            player.packetStateData.lastTeleportWasPotentiallyOnePointSeventeenDuplicate = false;
            if (!player.packetStateData.detectedStupidity && !wasPotentiallyOnePointSeventeenDuplicate) {
                // The player MUST send a stupidity packet before use item
                player.checkManager.getPacketCheck(BadPacketsU.class).flagAndAlert("type=skipped_stupid");
                return;
            }
        }

        player.packetStateData.lastTeleportWasPotentiallyOnePointSeventeenDuplicate = false;

        // If we received a believed stupidity packet, the next packet MUST be USE_ITEM.
        // If not, we were wrong or the client is attempting to fake stupidity.
        if (player.packetStateData.detectedStupidity || wasPotentiallyOnePointSeventeenDuplicate) {
            if (isUseItem(event)) {
                // Valid stupidity packet.
                player.packetStateData.detectedStupidity = false;

                // Possibly delay this USE_ITEM packet.
                player.checkManager.getPacketCheck(UseItemDelayer.class).addDelayed(event);
                player.packetStateData.stupidityRotChanged = false;

//                player.packetStateData.lastStupidity = null;
                return;
            }

            // Reset
            player.packetStateData.detectedStupidity = false;
            player.packetStateData.stupidityRotChanged = false;

            // There's not really much point doing this below here
            // I've left this in as originally it was intended to reduce falses from wrongly processing stupidity
            // It can also be bypassed in 5 minutes by sending a valid USE_ITEM packet
            // But there are issues with viaversion on 1.9 -> 1.8
            // You would also need to bump all priorities on the packet listeners to ensure this runs first

            // Let's ignore teleports
//            if (player.packetStateData.lastStupidity == null || wasPotentiallyOnePointSeventeenDuplicate) return; // Probably a teleport

            // We were wrong about it being stupidity, or the client is attempting to fake stupidity, reprocess this packet as non-stupidity
//            player.packetStateData.ignoreDuplicatePacket = true;
//
//            PacketEvents.getAPI().getPlayerManager().receivePacket(player.bukkitPlayer, new WrapperPlayClientPlayerFlying(true, true, player.packetStateData.packetPlayerOnGround, player.packetStateData.lastStupidity));
        }
    }

    private boolean isUseItem(PacketReceiveEvent event) {
        // This is USE_ITEM but translated by via
        if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_9) && event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
            return new WrapperPlayClientPlayerBlockPlacement(event).getFace() == BlockFace.OTHER;
        }
        return event.getPacketType() == PacketType.Play.Client.USE_ITEM;
    }
}
