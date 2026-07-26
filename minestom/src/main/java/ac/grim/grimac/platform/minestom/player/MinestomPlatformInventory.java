package ac.grim.grimac.platform.minestom.player;

import ac.grim.grimac.platform.api.player.PlatformInventory;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.entity.Player;

/**
 * Grim {@link PlatformInventory} view over a Minestom {@link Player}'s inventory + equipment,
 * exposing PacketEvents {@link ItemStack}s (see {@link MinestomItemStacks}).
 */
public final class MinestomPlatformInventory implements PlatformInventory {

    private final Player player;

    public MinestomPlatformInventory(Player player) {
        this.player = player;
    }

    private ItemStack equip(EquipmentSlot slot) {
        return MinestomItemStacks.toPe(player.getEquipment(slot));
    }

    @Override
    public ItemStack getItemInHand() {
        return equip(EquipmentSlot.MAIN_HAND);
    }

    @Override
    public ItemStack getItemInOffHand() {
        return equip(EquipmentSlot.OFF_HAND);
    }

    @Override
    public ItemStack getStack(int bukkitSlot, int vanillaSlot) {
        // TODO Phase 3: reconcile Grim's Bukkit/vanilla slot numbering with Minestom's; use the
        // vanilla index for now.
        return MinestomItemStacks.toPe(player.getInventory().getItemStack(vanillaSlot));
    }

    @Override
    public ItemStack getHelmet() {
        return equip(EquipmentSlot.HELMET);
    }

    @Override
    public ItemStack getChestplate() {
        return equip(EquipmentSlot.CHESTPLATE);
    }

    @Override
    public ItemStack getLeggings() {
        return equip(EquipmentSlot.LEGGINGS);
    }

    @Override
    public ItemStack getBoots() {
        return equip(EquipmentSlot.BOOTS);
    }

    @Override
    public ItemStack[] getContents() {
        net.minestom.server.item.ItemStack[] raw = player.getInventory().getItemStacks();
        ItemStack[] out = new ItemStack[raw.length];
        for (int i = 0; i < raw.length; i++) {
            out[i] = MinestomItemStacks.toPe(raw[i]);
        }
        return out;
    }

    @Override
    public String getOpenInventoryKey() {
        // TODO Phase 3: map the currently open Minestom inventory to Grim's expected key.
        return "";
    }
}
