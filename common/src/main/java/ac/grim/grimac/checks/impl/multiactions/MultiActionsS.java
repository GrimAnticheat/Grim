package ac.grim.grimac.checks.impl.multiactions;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PreViaPacketReceiveListener;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import org.jetbrains.annotations.NotNull;

@CheckData(name = "MultiActionsS", stableKey = "grim.multiactions.inventory_swing", description = "Swinging in an inventory", experimental = true)
public class MultiActionsS extends Check implements PreViaPacketReceiveListener {

    private final boolean dropsWithSwing = player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_15);
    private boolean swing;

    public MultiActionsS(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPreViaPacketReceive(@NotNull PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.ANIMATION) {
            if (swing) {
                flag();
            }

            if (dropsWithSwing && player.openWindow.mustBeOpen()) {
                swing = true;
                return;
            }

            if (player.openWindow.mustBeOpen() && flag() && shouldModifyPackets()) {
                event.setCancelled(true);
                player.onPacketCancel();
                player.closeInventory();
            }
        } else if (swing && !isAsync(event.getPacketType())) {
            swing = false;
            if (!isDrop(event)) {
                flag();
            }
        }
    }

    private static boolean isDrop(@NotNull PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.CLICK_WINDOW) return false;
        WrapperPlayClientClickWindow packet = new WrapperPlayClientClickWindow(event);
        return switch (packet.getWindowClickType()) {
            case PICKUP, QUICK_MOVE -> packet.getSlot() == -999;
            case THROW -> true;
            default -> false;
        };
    }
}
