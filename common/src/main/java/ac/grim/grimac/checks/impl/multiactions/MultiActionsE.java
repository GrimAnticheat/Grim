package ac.grim.grimac.checks.impl.multiactions;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;

@CheckData(name = "MultiActionsE", description = "Swinging while using an item", experimental = true)
public class MultiActionsE extends Check implements PacketCheck {
    public MultiActionsE(GrimPlayer player) {
        super(player);
    }

    private boolean dropping;

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!dropping && player.packetStateData.isSlowedByUsingItem() && (player.packetStateData.lastSlotSelected == player.packetStateData.getSlowedByUsingItemSlot() || player.packetStateData.eatingHand == InteractionHand.OFF_HAND) && event.getPacketType() == PacketType.Play.Client.ANIMATION) {
            // this is possible to false on 1.7
            if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_7_10)) {
                return;
            }

            if (flagAndAlert() && shouldModifyPackets()) {
                event.setCancelled(true);
                player.onPacketCancel();
            }
        }

        dropping = false;

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_15)) {
            DiggingAction action = new WrapperPlayClientPlayerDigging(event).getAction();
            dropping = action == DiggingAction.DROP_ITEM || action == DiggingAction.DROP_ITEM_STACK;
        }
    }
}
