package ac.grim.grimac.checks.impl.multiactions;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.packethandler.PacketHandlerRegistryBuilder;
import ac.grim.grimac.checks.type.PacketListener;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import org.jetbrains.annotations.NotNull;

@CheckData(name = "MultiActionsE", stableKey = "grim.multiactions.swing_while_using", description = "Swinging while using an item", experimental = true)
public class MultiActionsE extends Check implements PacketListener {
    private boolean dropping;

    public MultiActionsE(GrimPlayer player) {
        super(player);
    }

    @Override
    public void registerPacketHandlers(@NotNull PacketHandlerRegistryBuilder registry) {
        // this is possible to false on 1.7
        if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_7_10)) return;

        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_15)
                && player.getClientVersion().isOlderThan(ClientVersion.V_26_3)) {
            registry.registerPreViaReceiveHandler(event -> {
                if (isAnimation(event.getPacketType())) {
                    onAnimation(event);
                }

                if (!isAsync(event.getPacketType())) {
                    dropping = false;
                }

                if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
                    DiggingAction action = new WrapperPlayClientPlayerDigging(event).getAction();
                    dropping = action == DiggingAction.DROP_ITEM || action == DiggingAction.DROP_ITEM_STACK;
                }
            });
        } else {
            registry.registerPreViaReceiveHandler(this::onAnimation, ANIMATION_PACKETS);
        }
    }

    private void onAnimation(@NotNull PacketReceiveEvent event) {
        if (dropping || !player.packetStateData.isSlowedByUsingItem()
                || player.packetStateData.lastSlotSelected != player.packetStateData.getSlowedByUsingItemSlot()
                && player.packetStateData.itemInUseHand != InteractionHand.OFF_HAND) return;

        if (flag() && shouldModifyPackets()) {
            event.setCancelled(true);
            player.onPacketCancel();
        }
    }
}
