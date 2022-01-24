package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.data.AlmostBoolean;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.impl.PacketReceiveEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.enchantment.type.EnchantmentTypes;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientHeldItemChange;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

public class PacketPlayerDigging extends PacketListenerAbstract {

    public PacketPlayerDigging() {
        super(PacketListenerPriority.LOW);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {

            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer((Player) event.getPlayer());
            if (player == null) return;

            WrapperPlayClientPlayerDigging dig = new WrapperPlayClientPlayerDigging(event);

            if (dig.getAction() == DiggingAction.RELEASE_USE_ITEM) {
                player.packetStateData.slowedByUsingItem = AlmostBoolean.FALSE;
                player.packetStateData.slowedByUsingItemTransaction = player.lastTransactionReceived.get();

                if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_13)) {
                    ItemStack main = player.getInventory().getHeldItem();
                    ItemStack off = player.getInventory().getOffHand();

                    int j = 0;
                    if (main.getType() == ItemTypes.TRIDENT) {
                        j = main.getEnchantmentLevel(EnchantmentTypes.RIPTIDE);
                    } else if (off.getType() == ItemTypes.TRIDENT) {
                        j = off.getEnchantmentLevel(EnchantmentTypes.RIPTIDE);
                    }

                    if (j > 0) {
                        // TODO: Re-add riptide support
                        LogUtil.error("Riptide support is not yet implemented!");
                    }
                }
            }
        }

        if (event.getPacketType() == PacketType.Play.Client.HELD_ITEM_CHANGE) {
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer((Player) event.getPlayer());
            if (player == null) return;

            WrapperPlayClientHeldItemChange slot = new WrapperPlayClientHeldItemChange(event);

            // Stop people from spamming the server with out of bounds exceptions
            if (slot.getSlot() > 8) return;

            player.packetStateData.lastSlotSelected = slot.getSlot();
        }

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
            WrapperPlayClientPlayerBlockPlacement place = new WrapperPlayClientPlayerBlockPlacement(event);

            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer((Player) event.getPlayer());
            if (player == null) return;

            if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_8)
                    && player.gamemode == GameMode.SPECTATOR)
                return;

            // This was an interaction with a block, not a use item
            if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_9)
                    && place.getFace() != BlockFace.OTHER)
                return;

            player.packetStateData.slowedByUsingItemTransaction = player.lastTransactionReceived.get();

            ItemStack item = place.getHand() == InteractionHand.MAIN_HAND ?
                    player.getInventory().getHeldItem() : player.getInventory().getOffHand();

            if (item != null) {
                ItemType material = item.getType();

                if (player.checkManager.getCompensatedCooldown().hasMaterial(material)) {
                    player.packetStateData.slowedByUsingItem = AlmostBoolean.FALSE; // resync, not required
                    return; // The player has a cooldown, and therefore cannot use this item!
                }

                // 1.14 and below players cannot eat in creative, exceptions are potions or milk
                if ((player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_15) ||
                        player.gamemode != GameMode.CREATIVE && material.hasAttribute(ItemTypes.ItemAttribute.EDIBLE))
                        || material == ItemTypes.POTION || material == ItemTypes.MILK_BUCKET) {

                    // Pls have this mapped correctly retrooper
                    // TODO: Check if PacketEvents maps this oddity correctly
                    if (item.getType() == ItemTypes.SPLASH_POTION)
                        return;

                    // Eatable items that don't require any hunger to eat
                    if (material == ItemTypes.POTION || material == ItemTypes.MILK_BUCKET
                            || material == ItemTypes.GOLDEN_APPLE || material == ItemTypes.ENCHANTED_GOLDEN_APPLE
                            || material == ItemTypes.HONEY_BOTTLE) {
                        player.packetStateData.slowedByUsingItem = AlmostBoolean.TRUE;
                        player.packetStateData.eatingHand = place.getHand();

                        return;
                    }

                    // The other items that do require it
                    // TODO: Food level lag compensation
                    if (item.getType().hasAttribute(ItemTypes.ItemAttribute.EDIBLE) &&
                            (((Player) event.getPlayer()).getFoodLevel() < 20 || player.gamemode == GameMode.CREATIVE)) {
                        player.packetStateData.slowedByUsingItem = AlmostBoolean.TRUE;
                        player.packetStateData.eatingHand = place.getHand();

                        return;
                    }

                    // The player cannot eat this item, resync use status
                    player.packetStateData.slowedByUsingItem = AlmostBoolean.FALSE;
                }

                if (material == ItemTypes.SHIELD) {
                    player.packetStateData.slowedByUsingItem = AlmostBoolean.TRUE;
                    player.packetStateData.eatingHand = place.getHand();

                    return;
                }

                // Avoid releasing crossbow as being seen as slowing player
                if (material == ItemTypes.CROSSBOW && item.getNBT().getBoolean("Charged")) {
                    return;
                }

                // The client and server don't agree on trident status because mojang is incompetent at netcode.
                if (material == ItemTypes.TRIDENT) {
                    if (item.getEnchantmentLevel(EnchantmentTypes.RIPTIDE) > 0)
                        player.packetStateData.slowedByUsingItem = AlmostBoolean.MAYBE;
                    else
                        player.packetStateData.slowedByUsingItem = AlmostBoolean.TRUE;
                    player.packetStateData.eatingHand = place.getHand();
                }

                // Players in survival can't use a bow without an arrow
                // Crossbow charge checked previously
                if (material == ItemTypes.BOW || material == ItemTypes.CROSSBOW) {
                    player.packetStateData.slowedByUsingItem = player.gamemode == GameMode.CREATIVE ||
                            player.getInventory().hasItemType(ItemTypes.ARROW) ||
                            player.getInventory().hasItemType(ItemTypes.TIPPED_ARROW) ||
                            player.getInventory().hasItemType(ItemTypes.SPECTRAL_ARROW) ? AlmostBoolean.TRUE : AlmostBoolean.FALSE;
                    player.packetStateData.eatingHand = place.getHand();
                }

                // Only 1.8 and below players can block with swords
                if (material.toString().endsWith("_SWORD")) {
                    if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8))
                        player.packetStateData.slowedByUsingItem = AlmostBoolean.TRUE;
                    else if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_9)) // ViaVersion stuff
                        player.packetStateData.slowedByUsingItem = AlmostBoolean.MAYBE;
                }
            } else {
                player.packetStateData.slowedByUsingItem = AlmostBoolean.FALSE;
            }
        }
    }
}
