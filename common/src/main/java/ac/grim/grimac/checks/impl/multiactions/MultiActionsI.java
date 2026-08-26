package ac.grim.grimac.checks.impl.multiactions;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PreViaPacketReceiveListener;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import org.jetbrains.annotations.NotNull;

@CheckData(name = "MultiActionsI", stableKey = "grim.multiactions.inventory_attack", description = "Interacted with an entity while in an inventory", experimental = true)
public class MultiActionsI extends Check implements PreViaPacketReceiveListener {

    public MultiActionsI(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPreViaPacketReceive(@NotNull PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY
                || event.getPacketType() == PacketType.Play.Client.ATTACK
                || event.getPacketType() == PacketType.Play.Client.SPECTATE_ENTITY
                || event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING
                && new WrapperPlayClientPlayerDigging(event).getAction() == DiggingAction.STAB) {
            if (player.openWindow.mustBeOpen() && flag() && shouldModifyPackets()) {
                event.setCancelled(true);
                player.onPacketCancel();
                player.closeInventory();
            }
        }
    }
}
