package ac.grim.grimac.utils;

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

// TODO: move this to a sub-package of utils
public class AttackCooldownHandler extends Check implements PacketCheck {
    private int ticksSinceLastSwing;
    private ItemStack stack = ItemStack.EMPTY;

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
            ++ticksSinceLastSwing;

            // FIXME:
            //  this is the only part which can cause falses, since this doesn't run every client tick.
            //  for example, if the player switches slots while standing still and then back to the original one
            //  before the next tick packet, the cooldown will get reset on the client but won't get reset here.
            //  this could be also be run on transactions to mitigate this, but that could also cause issues

            ItemStack held = player.inventory.getHeldItem().copy();

            if (!(stack.isEmpty() && held.isEmpty() || stack.getType() == held.getType() && (stack.isDamageableItem() || stack.getLegacyData() == held.getLegacyData()))) {
                reset();
            }

            stack = held;
        }
    }

    public void reset() {
        ticksSinceLastSwing = 0;
    }

    public float getMinimumProgress() {
        return GrimMath.clamp(((float) ticksSinceLastSwing + 0.5F) / (float) (1d / player.compensatedEntities.self.getAttributeValue(Attributes.ATTACK_SPEED) * 20.0D), 0, 1);
    }
}
