package ac.grim.grimac.checks.impl.multiactions;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.packethandler.PacketHandlerRegistryBuilder;
import ac.grim.grimac.checks.type.PacketListener;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import org.jetbrains.annotations.NotNull;

@CheckData(name = "MultiActionsA", stableKey = "grim.multiactions.attack_while_using", description = "Attacked while using an item", experimental = true)
public class MultiActionsA extends Check implements PacketListener {
    public MultiActionsA(GrimPlayer player) {
        super(player);
    }

    @Override
    public void registerPacketHandlers(@NotNull PacketHandlerRegistryBuilder registry) {
        registry.registerPreViaReceiveHandler(this::onAttack, PacketType.Play.Client.ATTACK, PacketType.Play.Client.SPECTATE_ENTITY);
        registry.registerPreViaReceiveHandler(event -> {
            if (new WrapperPlayClientInteractEntity(event).getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                onAttack(event);
            }
        }, PacketType.Play.Client.INTERACT_ENTITY);
        registry.registerPreViaReceiveHandler(event -> {
            if (new WrapperPlayClientPlayerDigging(event).getAction() == DiggingAction.STAB) {
                onAttack(event);
            }
        }, PacketType.Play.Client.PLAYER_DIGGING);
    }

    private void onAttack(@NotNull PacketReceiveEvent event) {
        if (!player.packetStateData.isSlowedByUsingItem()) return;

        if (player.packetStateData.lastSlotSelected != player.packetStateData.getSlowedByUsingItemSlot()
                && player.packetStateData.itemInUseHand != InteractionHand.OFF_HAND) {
            return;
        }

        if (flag() && shouldModifyPackets()) {
            event.setCancelled(true);
            player.onPacketCancel();
        }
    }
}
