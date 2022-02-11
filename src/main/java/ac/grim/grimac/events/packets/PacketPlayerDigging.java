package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.enchantment.type.EnchantmentTypes;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientHeldItemChange;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUseItem;
import org.bukkit.GameMode;

public class PacketPlayerDigging extends PacketListenerAbstract {

    public PacketPlayerDigging() {
        super(PacketListenerPriority.LOW, true);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {

            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;

            WrapperPlayClientPlayerDigging dig = new WrapperPlayClientPlayerDigging(event);

            if (dig.getAction() == DiggingAction.RELEASE_USE_ITEM) {
                player.packetStateData.slowedByUsingItem = false;
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
                        // TODO: Check if player has fast use item
                        player.tryingToRiptide = true;
                    }
                }
            }
        }

        if (event.getPacketType() == PacketType.Play.Client.HELD_ITEM_CHANGE) {
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;

            WrapperPlayClientHeldItemChange slot = new WrapperPlayClientHeldItemChange(event);

            // Stop people from spamming the server with out of bounds exceptions
            if (slot.getSlot() > 8) return;

            player.packetStateData.lastSlotSelected = slot.getSlot();
        }

        if (event.getPacketType() == PacketType.Play.Client.USE_ITEM) {
            WrapperPlayClientUseItem place = new WrapperPlayClientUseItem(event);

            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;

            if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_8)
                    && player.gamemode == GameMode.SPECTATOR)
                return;

            // This was an interaction with a block, not a use item
            // TODO: What is 1.8 doing with packets?  I think it's BLOCK_PLACE not USE_ITEM
            if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_9))
                return;

            player.packetStateData.slowedByUsingItemTransaction = player.lastTransactionReceived.get();

            ItemStack item = place.getHand() == InteractionHand.MAIN_HAND ?
                    player.getInventory().getHeldItem() : player.getInventory().getOffHand();

            if (item != null) {
                ItemType material = item.getType();

                if (player.checkManager.getCompensatedCooldown().hasMaterial(material)) {
                    player.packetStateData.slowedByUsingItem = false; // resync, not required
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
                        player.packetStateData.slowedByUsingItem = true;
                        player.packetStateData.eatingHand = place.getHand();

                        return;
                    }

                    // The other items that do require it
                    // TODO: Food level lag compensation
                    if (item.getType().hasAttribute(ItemTypes.ItemAttribute.EDIBLE) && (player.bukkitPlayer.getFoodLevel() < 20 || player.gamemode == GameMode.CREATIVE)) {
                        player.packetStateData.slowedByUsingItem = true;
                        player.packetStateData.eatingHand = place.getHand();

                        return;
                    }

                    // The player cannot eat this item, resync use status
                    player.packetStateData.slowedByUsingItem = false;
                }

                if (material == ItemTypes.SHIELD) {
                    player.packetStateData.slowedByUsingItem = true;
                    player.packetStateData.eatingHand = place.getHand();

                    return;
                }

                // Avoid releasing crossbow as being seen as slowing player
                if (material == ItemTypes.CROSSBOW && item.getNBT().getBoolean("Charged")) {
                    player.packetStateData.slowedByUsingItem = false; // TODO: Fix this
                    return;
                }

                // The client and server don't agree on trident status because mojang is incompetent at netcode.
                if (material == ItemTypes.TRIDENT) {
                    player.packetStateData.slowedByUsingItem = item.getEnchantmentLevel(EnchantmentTypes.RIPTIDE) <= 0;
                    player.packetStateData.eatingHand = place.getHand();
                }

                // Players in survival can't use a bow without an arrow
                // Crossbow charge checked previously
                if (material == ItemTypes.BOW || material == ItemTypes.CROSSBOW) {
                    /*player.packetStateData.slowedByUsingItem = player.gamemode == GameMode.CREATIVE ||
                            player.getInventory().hasItemType(ItemTypes.ARROW) ||
                            player.getInventory().hasItemType(ItemTypes.TIPPED_ARROW) ||
                            player.getInventory().hasItemType(ItemTypes.SPECTRAL_ARROW);
                    player.packetStateData.eatingHand = place.getHand();*/
                    // TODO: How do we lag compensate arrows? Mojang removed idle packet.
                    player.packetStateData.slowedByUsingItem = false;
                }

                // Only 1.8 and below players can block with swords
                if (material.toString().endsWith("_SWORD")) {
                    if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8))
                        player.packetStateData.slowedByUsingItem = true;
                    else if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_9)) // ViaVersion stuff
                        player.packetStateData.slowedByUsingItem = false;
                }
            } else {
                player.packetStateData.slowedByUsingItem = false;
            }
        }
    }
}
