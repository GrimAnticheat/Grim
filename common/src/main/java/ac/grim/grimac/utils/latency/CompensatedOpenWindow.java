package ac.grim.grimac.utils.latency;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.type.PacketReceiveListener;
import ac.grim.grimac.checks.type.PreViaPacketReceiveListener;
import ac.grim.grimac.checks.type.PreViaPacketSendListener;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.data.TrackerData;
import ac.grim.grimac.utils.inventory.inventory.MenuType;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClientStatus;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerOpenHorseWindow;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerOpenWindow;
import lombok.Getter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

// TODO: books, signs, etc
public class CompensatedOpenWindow extends Check implements PreViaPacketReceiveListener, PreViaPacketSendListener, PacketReceiveListener {

    private static final Window PLAYER_INVENTORY = new Window(0, Long.MIN_VALUE, null);

    private final HashSet<@Nullable Window> possibleOpenWindows = new HashSet<>(2);
    public final boolean clientSendsOpenInventoryPacket = player.getClientVersion().isOlderThan(ClientVersion.V_1_12);
    private long openPlayerInventoryTransaction = Long.MIN_VALUE;
    private long closeTransaction = Long.MIN_VALUE;
    @Getter
    private int ticksOpen;

    public CompensatedOpenWindow(@NotNull GrimPlayer player) {
        super(player);
        possibleOpenWindows.add(null);
    }

    @Override
    public void onPreViaPacketReceive(@NotNull PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CLIENT_STATUS && clientSendsOpenInventoryPacket) {
            WrapperPlayClientClientStatus packet = new WrapperPlayClientClientStatus(event);
            if (packet.getAction() == WrapperPlayClientClientStatus.Action.OPEN_INVENTORY_ACHIEVEMENT) {
                onClientOpenOrCloseWindow(true);
            }
        } else if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW && !clientSendsOpenInventoryPacket) {
            WrapperPlayClientClickWindow packet = new WrapperPlayClientClickWindow(event);
            if (packet.getWindowId() == 0) {
                onClientOpenOrCloseWindow(true);
            }
        } else if (event.getPacketType() == PacketType.Play.Client.CLOSE_WINDOW) {
            onClientOpenOrCloseWindow(false);
        } else if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            if (player.pointThreeEstimator.isNearPortal && player.getClientVersion().isOlderThan(ClientVersion.V_1_12_2) && !player.inVehicle()) {
                maybeClose();
            }
        }
    }

    @Override
    public void onPreViaPacketSend(@NotNull PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.CLOSE_WINDOW) {
            handleServerOpenWindow(event, 0, null);
        } else if (event.getPacketType() == PacketType.Play.Server.OPEN_WINDOW) {
            WrapperPlayServerOpenWindow packet = new WrapperPlayServerOpenWindow(event);
            int windowId = packet.getContainerId();
            MenuType type = MenuType.getMenuType(packet, player.getClientVersion());
            if (type == null) {
                // one of 2 cases:
                // 1. the client ignores this
                // 2. the previous window's id is changed or client-side NPE
                event.setCancelled(true);
                LogUtil.warn("Unknown window type for version " + player.getClientVersion().getReleaseName()
                        + ": " + (packet.getLegacyType() != null ? packet.getLegacyType() : packet.getType()));
                return;
            }
            if (type == MenuType.HORSE) {
                openHorseWindow(event, windowId, packet.getHorseId());
            } else {
                handleServerOpenWindow(event, windowId, type);
            }
        } else if (event.getPacketType() == PacketType.Play.Server.OPEN_HORSE_WINDOW) {
            WrapperPlayServerOpenHorseWindow packet = new WrapperPlayServerOpenHorseWindow(event);
            openHorseWindow(event, packet.getWindowId(), packet.getEntityId());
        }
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (isTickPacket(event.getPacketType())) {
            if (mustBeOpen()) {
                ticksOpen++;
            } else {
                ticksOpen = 0;
            }
        }
    }

    private void openHorseWindow(@NotNull PacketSendEvent event, int windowId, int entityId) {
        TrackerData entity = player.compensatedEntities.serverPositionsMap.get(entityId);
        if (entity == null || !entity.getEntityType().isInstanceOf(EntityTypes.ABSTRACT_HORSE))
            return; // client ignores this
        handleServerOpenWindow(event, windowId, MenuType.HORSE);
    }

    private void handleServerOpenWindow(@NotNull PacketSendEvent event, int windowId, MenuType type) {
        player.sendTransaction();
        event.getTasksAfterSend().add(player::sendTransaction);
        int transaction = player.getLastTransactionSent();
        Window window = type == null ? null : new Window(windowId, transaction, type);
        player.addRealTimeTaskNow(() -> possibleOpenWindows.add(window));
        player.addRealTimeTaskNext(() -> closeBefore(transaction, window != null));
    }

    private void onClientOpenOrCloseWindow(boolean open) {
        int transaction = player.getLastTransactionReceived();
        possibleOpenWindows.removeIf(it -> it == null || it == PLAYER_INVENTORY
                // not equals because they might not have gotten it yet!
                || it.transaction < transaction);
        possibleOpenWindows.add(open ? PLAYER_INVENTORY : null);
        if (open) {
            openPlayerInventoryTransaction = transaction;
        } else {
            ticksOpen = 0;
            closeTransaction = transaction;
        }
    }

    private void closeBefore(int transaction, boolean closeEmpty) {
        if (!closeEmpty) {
            closeTransaction = player.getLastTransactionReceived();
            ticksOpen = 0;
        }
        boolean shouldCloseEmpty = closeEmpty && closeTransaction <= transaction;
        boolean shouldClosePlayer = openPlayerInventoryTransaction <= transaction;
        possibleOpenWindows.removeIf(it -> it == PLAYER_INVENTORY ? shouldClosePlayer
                : it == null ? shouldCloseEmpty : it.transaction < transaction);
    }

    public void maybeClose() {
        possibleOpenWindows.add(null);
        closeTransaction = player.getLastTransactionReceived();
        ticksOpen = 0;
    }

    public void closeFromRespawn() {
        int transaction = player.getLastTransactionReceived() - 1;
        possibleOpenWindows.removeIf(it -> it == null
                // what if the player opens the inventory during a transaction split? TODO: test; this *should* fix it
                || it == PLAYER_INVENTORY && openPlayerInventoryTransaction == transaction
                // not equals because they might not have gotten it yet!
                || it.transaction < transaction);
        possibleOpenWindows.add(null);
        ticksOpen = 0;
    }

    public boolean mustBeOpen() {
        for (Window window : possibleOpenWindows) {
            if (window == null || window.canDesync(player)) return false;
        }

        return true;
    }

    public Set<@Nullable Window> getPossibilities() {
        return new HashSet<>(possibleOpenWindows);
    }

    public record Window(int id, long transaction, @Nullable MenuType type) {
        @Contract(pure = true)
        public boolean canDesync(@NotNull GrimPlayer player) {
            return player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8) && type == MenuType.BEACON;
        }
    }
}
