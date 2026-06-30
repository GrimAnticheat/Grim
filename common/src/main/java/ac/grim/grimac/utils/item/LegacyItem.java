package ac.grim.grimac.utils.item;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.latency.CompensatedWorld;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.component.builtin.item.FoodProperties;
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemConsumable;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.enchantment.type.EnchantmentTypes;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;

public class LegacyItem extends ItemBehaviour {

    public static final LegacyItem INSTANCE = new LegacyItem();

    @Override
    public boolean canUse(ItemStack item, CompensatedWorld world, GrimPlayer player, InteractionHand hand) {
        final ItemType material = item.getType();

        // Check for data component stuff on 1.21.2+
        final ItemConsumable consumable = item.getComponentOr(ComponentTypes.CONSUMABLE, null);
        final FoodProperties foodComponent = item.getComponentOr(ComponentTypes.FOOD, null);

        // The food component can override the consumable component, as it provides conditions for using the item
        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21_2) && consumable != null && foodComponent == null) {
            return true;
        }

        // Check for data component stuff on 1.20.5+
        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_20_5) && foodComponent != null) {
            if (foodComponent.isCanAlwaysEat() || player.food < 20 || player.gamemode == GameMode.CREATIVE) {
                return true;
            }
        }

        // 1.14 and below players cannot eat in creative, exceptions are potions or milk
        if (material.hasAttribute(ItemTypes.ItemAttribute.EDIBLE) &&
                (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_15) || player.gamemode != GameMode.CREATIVE)
                || material == ItemTypes.POTION || material == ItemTypes.MILK_BUCKET) {

            // Pls have this mapped correctly retrooper
            if (material == ItemTypes.SPLASH_POTION)
                return false;
            // 1.8 splash potion
            if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_9) && item.getLegacyData() > 16384) {
                return false;
            }

            // Eatable items that don't require any hunger to eat
            if (material == ItemTypes.POTION || material == ItemTypes.MILK_BUCKET
                    || material == ItemTypes.GOLDEN_APPLE || material == ItemTypes.ENCHANTED_GOLDEN_APPLE
                    || material == ItemTypes.HONEY_BOTTLE || material == ItemTypes.SUSPICIOUS_STEW ||
                    material == ItemTypes.CHORUS_FRUIT) {
                return true;
            }

            // The other items that do require it
            if (material.hasAttribute(ItemTypes.ItemAttribute.EDIBLE) && ((player.platformPlayer != null && player.food < 20) || player.gamemode == GameMode.CREATIVE)) {
                return true;
            }
        }

        if (material == ItemTypes.SHIELD && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9)) {
            return true;
        }

        // Avoid releasing crossbow as being seen as slowing player
        final NBTCompound nbt = item.getNBT(); // How can this be null?
        if (material == ItemTypes.CROSSBOW && nbt != null && nbt.getBoolean("Charged")) {
            return false; // TODO: Fix this
        }

        // The client and server don't agree on trident status because mojang is incompetent at netcode.
        if (material == ItemTypes.TRIDENT
                && item.getDamageValue() < item.getMaxDamage() - 1 // Player can't use item if it's "about to break"
                && (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_13_2)
                || player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8))) {
            return item.getEnchantmentLevel(EnchantmentTypes.RIPTIDE, PacketEvents.getAPI().getServerManager().getVersion().toClientVersion()) <= 0;
        }

        // Players in survival can't use a bow without an arrow
        // Crossbow charge checked previously
        if (material == ItemTypes.BOW || material == ItemTypes.CROSSBOW) {
            // TODO: How do we lag compensate arrows? Mojang removed idle packet.
            // I think we may have to cancel the bukkit event if the player isn't slowed
            // On 1.8, it wouldn't be too bad to handle bows correctly
            // But on 1.9+, no idle packet and clients/servers don't agree on bow status
            // Mojang pls fix
            return false;
        }

        if (material == ItemTypes.SPYGLASS && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_17)) {
            return true;
        }

        if (material == ItemTypes.GOAT_HORN && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_19)) {
            return true;
        }

        // Only 1.8 and below players can block with swords
        if (material.hasAttribute(ItemTypes.ItemAttribute.SWORD)) {
            if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8)) {
                return true;
            } else if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_9)) { // ViaVersion stuff
                return player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21_4);
            }
        }

        return false;
    }

}
