package ac.grim.grimac.platform.minestom.player;

import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;

/**
 * Minestom {@code ItemStack} → PacketEvents {@link ItemStack} conversion.
 * <p>
 * PHASE 2 SCOPE: type + amount only (enough for checks that key off the held/armor item
 * type). TODO Phase 3: carry over components (enchants, damage, food, etc.) that some checks
 * read; map via the item's NBT/components once those checks are exercised.
 */
final class MinestomItemStacks {

    private MinestomItemStacks() {
    }

    static ItemStack toPe(net.minestom.server.item.ItemStack minestom) {
        if (minestom == null || minestom.isAir()) {
            return ItemStack.EMPTY;
        }
        try {
            ItemType type = ItemTypes.getByName(minestom.material().key().asString());
            if (type == null) {
                return ItemStack.EMPTY;
            }
            return ItemStack.builder().type(type).amount(minestom.amount()).build();
        } catch (RuntimeException ignored) {
            return ItemStack.EMPTY;
        }
    }
}
