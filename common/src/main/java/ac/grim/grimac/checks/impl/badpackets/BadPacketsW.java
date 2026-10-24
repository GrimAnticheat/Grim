package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.api.storage.verbose.Verbose;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketReceiveListener;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.latency.CompensatedOpenWindow;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import org.jetbrains.annotations.NotNull;

@CheckData(name = "BadPacketsW", stableKey = "grim.badpackets.wrong_window", description = "Clicked in an unopen window")
public class BadPacketsW extends Check implements PacketReceiveListener {
    private static final Verbose V = Verbose.of("windowId={sint}");

    public BadPacketsW(final GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(@NotNull PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.CLICK_WINDOW) return;

        WrapperPlayClientClickWindow packet = new WrapperPlayClientClickWindow(event);
        int windowId = packet.getWindowId();

        if (windowId == 0 && !player.openWindow.clientSendsOpenInventoryPacket && !player.openWindow.mustBeOpen()) {
            return;
        }

        for (CompensatedOpenWindow.Window window : player.openWindow.getPossibilities()) {
            if (window != null && window.id() == windowId) return;
        }

        if (flag(V.write(verbose()).sint(windowId)) && shouldModifyPackets()) {
            event.setCancelled(true);
            player.onPacketCancel();
            player.closeInventory();
        }
    }
}
