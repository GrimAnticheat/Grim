package ac.grim.grimac.manager;

import ac.grim.grimac.checks.GrimProcessor;
import ac.grim.grimac.checks.packethandler.PacketHandlerRegistryBuilder;
import ac.grim.grimac.checks.type.BlockBreakListener;
import ac.grim.grimac.checks.type.PacketListener;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockBreak;
import ac.grim.grimac.utils.math.GrimMath;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import org.jetbrains.annotations.NotNull;

public class AttackCooldownHandler extends GrimProcessor implements PacketListener, BlockBreakListener {
    private int ticksSinceLastSwing;
    private ItemStack stack = ItemStack.EMPTY;
    // Since we don't know when the client ticks, we call updateHeldItem() when the held item changes,
    // but that means ticksSinceLastSwing is incremented after it gets reset. This is wrong, so we
    // compensate for that by not incrementing ticksSinceLastSwing if updateHeldItem() was called
    // (and the held item changed) before the tick packet.
    private boolean stackChanged;

    public AttackCooldownHandler(GrimPlayer player) {
        super(player);
    }

    @Override
    public void registerPacketHandlers(@NotNull PacketHandlerRegistryBuilder registry) {
        registry.registerReceiveHandler(event -> {
            // FIXME: should only run when the click misses
            reset();
        }, ANIMATION_PACKETS);

        registry.registerReceiveHandler(event -> {
            if (!isTickPacket(event.getPacketType())) return;

            if (!stackChanged) {
                ++ticksSinceLastSwing;
            }
            updateHeldItem(true);
            stackChanged = false;
        }, TICK_PACKET_CANDIDATES);
    }

    @Override
    public void onBlockBreak(BlockBreak blockBreak) {
        if (blockBreak.action == DiggingAction.CANCELLED_DIGGING && blockBreak.face == BlockFace.DOWN) {
            // FIXME: this could also be triggered by switching targets while looking at the bottom face of a block
            reset();
        }
    }

    public void reset() {
        ticksSinceLastSwing = 0;
    }

    // called on client tick and whenever the slot gets updated
    public void updateHeldItem(boolean tick) {
        if (!tick && player.supportsEndTick()) return;
        ItemStack held = player.inventory.getHeldItem().copy();

        if (!(stack.isEmpty() && held.isEmpty() || stack.getType() == held.getType() && (stack.isDamageableItem() || stack.getLegacyData() == held.getLegacyData()))) {
            reset();
            stackChanged = true;
        }

        stack = held;
    }

    public float getMinimumProgress() {
        return GrimMath.clamp(((float) ticksSinceLastSwing + 0.5F) / (float) (1d / player.compensatedEntities.self.getAttributeValue(Attributes.ATTACK_SPEED) * 20.0D), 0, 1);
    }


}
