package ac.grim.grimac.events.packets.patch;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.lists.EvictingQueue;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.netty.channel.ChannelHelper;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Locale;

// For 1.9+ clients -> 1.8 servers only.
public class UseItemDelayer extends Check implements PacketCheck {

    // EvictingQueue to prevent spamming this
    private final EvictingQueue<DelayedUseItem> delayedUseItemPackets = new EvictingQueue<>(20);

    public UseItemDelayer(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        // If we received a normal flying packet
        // Use item packets are delayed until we have a valid flying packet
        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType()) && !player.packetStateData.lastPacketWasOnePointSeventeenDuplicate) {
            for (DelayedUseItem delayedUseItemPacket : delayedUseItemPackets) {
                PacketWrapper<?> packet = new WrapperPlayClientPlayerBlockPlacement(delayedUseItemPacket.interactionHand, delayedUseItemPacket.blockPosition, delayedUseItemPacket.face, delayedUseItemPacket.cursorPosition, delayedUseItemPacket.itemStack, delayedUseItemPacket.insideBlock, delayedUseItemPacket.sequence);

                // Receive after flying has been received by the server
                // PE doesn't seem to have a proper way to do this, but this works only with runInEventLoop.
                event.getPostTasks().add(() -> ChannelHelper.runInEventLoop(player.user.getChannel(), () -> {
                    PacketEvents.getAPI().getPlayerManager().receivePacketSilently(player.bukkitPlayer, packet);
                }));
            }
            delayedUseItemPackets.clear();
        }
    }

    public void addDelayed(PacketReceiveEvent event) {
        // Only delay if rotation changed.
        if (!player.packetStateData.stupidityRotChanged) {
            return;
        }

        InteractionHand hand;
        DelayedUseItem delayed;

        // This is USE_ITEM but translated by via
        final WrapperPlayClientPlayerBlockPlacement placement = new WrapperPlayClientPlayerBlockPlacement(event);
        hand = placement.getHand();
        delayed = new DelayedUseItem(placement.getHand(), placement.getBlockPosition(), placement.getFace(), placement.getCursorPosition(), placement.getItemStack().orElse(null), placement.getInsideBlock().orElse(null), placement.getSequence());

        // Only delay if it comes from a bucket item, as this is the purpose of the stupidity packet.
        // Otherwise just allow use item to pass instantly
        ItemStack stack = hand == InteractionHand.MAIN_HAND ? player.getInventory().getHeldItem() : player.getInventory().getOffHand();
        if (!stack.getType().getName().getKey().toLowerCase(Locale.ROOT).contains("bucket")) return;

        delayedUseItemPackets.add(delayed);
        event.setCancelled(true);
    }

    @Data
    @AllArgsConstructor
    private static class DelayedUseItem {
        private InteractionHand interactionHand;
        private Vector3i blockPosition;
        private BlockFace face;
        private Vector3f cursorPosition;
        private ItemStack itemStack;
        private Boolean insideBlock;
        private int sequence;
    }
}
