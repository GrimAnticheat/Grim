package ac.grim.grimac.manager;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.math.GrimMath;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;

public class AttackCooldownHandler extends Check implements PacketCheck {
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
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.ANIMATION) {
            // FIXME: should only run when the click misses
            reset();
        }

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            WrapperPlayClientPlayerDigging packet = new WrapperPlayClientPlayerDigging(event);
            if (packet.getAction() == DiggingAction.CANCELLED_DIGGING && packet.getBlockFace() == BlockFace.DOWN) {
                // FIXME: this could also be triggered by switching targets while looking at the bottom face of a block
                reset();
            }
        }

        if (isTickPacket(event.getPacketType())) {
            if (!stackChanged) {
                ++ticksSinceLastSwing;
            }
            updateHeldItem();
            stackChanged = false;
        }
    }

    public void reset() {
        ticksSinceLastSwing = 0;
    }

    // called on client tick and whenever the slot gets updated
    public void updateHeldItem() {
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
