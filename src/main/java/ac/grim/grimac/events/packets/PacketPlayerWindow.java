package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.packetentity.PacketEntitySelf;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClientStatus;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClientStatus.Action;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerOpenWindow;

public class PacketPlayerWindow extends PacketListenerAbstract {

    public PacketPlayerWindow() {
        super(PacketListenerPriority.LOWEST);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        // This designed only to check if player going into nether portal with opened inventory
        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType()) && !event.isCancelled()) {
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;

            if(player.hasInventoryOpen && isDesynced(player)) {
                player.hasInventoryOpen = false;
            }
        }

        // Client Status is sent in 1.7-1.8
        if (event.getPacketType() == PacketType.Play.Client.CLIENT_STATUS) {
            WrapperPlayClientClientStatus wrapper = new WrapperPlayClientClientStatus(event);

            if (wrapper.getAction() == Action.OPEN_INVENTORY_ACHIEVEMENT) {
                GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
                if (player == null) return;

                player.hasInventoryOpen = true;
            }
        }

        // We need to do this due to 1.9 not sending anymore the inventory action in the Client Status
        if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;

            if (player.getClientVersion().isNewerThan(ClientVersion.V_1_8)) {
                player.hasInventoryOpen = true;
            }
        }

        if (event.getPacketType() == PacketType.Play.Client.CLOSE_WINDOW) {
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;

            player.hasInventoryOpen = false;
        }
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.RESPAWN) {
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;

            player.sendTransaction();

            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(),
                                                () -> player.hasInventoryOpen = false);
        } else if (event.getPacketType() == PacketType.Play.Server.OPEN_WINDOW) {
            WrapperPlayServerOpenWindow wrapper = new WrapperPlayServerOpenWindow(event);

            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;

            player.sendTransaction();

            String legacyType = wrapper.getLegacyType();
            int modernType = wrapper.getType();

            // Exempt beacons due to desyncs
            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(),
                                                () -> player.hasInventoryOpen = !isAlwaysDesynced(player, legacyType, modernType));
        } else if (event.getPacketType() == PacketType.Play.Server.OPEN_HORSE_WINDOW) {
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;

            player.sendTransaction();

            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(),
                                                () -> player.hasInventoryOpen = true);
        } else if (event.getPacketType() == PacketType.Play.Server.CLOSE_WINDOW) {
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;

            player.sendTransaction();

            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(),
                                                () -> player.hasInventoryOpen = false);
        }
    }

    private boolean isAlwaysDesynced(GrimPlayer player, String legacyType, int modernType) {
        // Closing beacon with the GUI button causing desync, fixed in 1.9
        if (player.getClientVersion() == ClientVersion.V_1_8 &&
                ("minecraft:beacon".equals(legacyType) || modernType == 8)) {
            return true;
        }

        return isDesynced(player);
    }

    private boolean isDesynced(GrimPlayer player) {
        // Going inside nether portal with opened chest will cause desync, fixed in 1.12.2
        if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_12_1) &&
                player.pointThreeEstimator.isNearNetherPortal) {
            PacketEntitySelf playerEntity = player.compensatedEntities.getSelf();
            // Client ignore nether portal if player has passengers or riding an entity
            if (!playerEntity.inVehicle() && playerEntity.passengers.isEmpty()) {
                return true;
            }
        }

        return false;
    }
}