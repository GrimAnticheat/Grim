package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.checks.impl.movement.NoSlow;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.component.builtin.item.FoodProperties;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.enchantment.type.EnchantmentTypes;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.wrapper.play.client.*;

public class PacketPlayerDigging extends PacketListenerAbstract {

    public PacketPlayerDigging() {
        super(PacketListenerPriority.LOW);
    }

    public static void handleUseItem(GrimPlayer player, ItemStack item, InteractionHand hand) {
        if (item == null) {
            player.packetStateData.setSlowedByUsingItem(false);
            return;
        }

        final ItemType material = item.getType();

        if (player.checkManager.getCompensatedCooldown().hasMaterial(material)) {
            player.packetStateData.setSlowedByUsingItem(false); // resync, not required
            return; // The player has a cooldown, and therefore cannot use this item!
        }

        // Check for data component stuff on 1.20.5+
        final FoodProperties foodComponent = item.getComponentOr(ComponentTypes.FOOD, null);
        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_20_5) && foodComponent != null) {
            if (foodComponent.isCanAlwaysEat() || player.food < 20 || player.gamemode == GameMode.CREATIVE) {
                player.packetStateData.setSlowedByUsingItem(true);
                player.packetStateData.eatingHand = hand;
                return;
            } else {
                player.packetStateData.setSlowedByUsingItem(false);
            }
        }

        // 1.14 and below players cannot eat in creative, exceptions are potions or milk
        if (material.hasAttribute(ItemTypes.ItemAttribute.EDIBLE) &&
                (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_15) || player.gamemode != GameMode.CREATIVE)
                || material == ItemTypes.POTION || material == ItemTypes.MILK_BUCKET) {

            // Pls have this mapped correctly retrooper
            if (item.getType() == ItemTypes.SPLASH_POTION)
                return;
            // 1.8 splash potion
            if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_9) && item.getLegacyData() > 16384) {
                return;
            }

            // Eatable items that don't require any hunger to eat
            if (material == ItemTypes.POTION || material == ItemTypes.MILK_BUCKET
                    || material == ItemTypes.GOLDEN_APPLE || material == ItemTypes.ENCHANTED_GOLDEN_APPLE
                    || material == ItemTypes.HONEY_BOTTLE || material == ItemTypes.SUSPICIOUS_STEW ||
                    material == ItemTypes.CHORUS_FRUIT) {
                player.packetStateData.setSlowedByUsingItem(true);
                player.packetStateData.eatingHand = hand;
                return;
            }

            // The other items that do require it
            if (item.getType().hasAttribute(ItemTypes.ItemAttribute.EDIBLE) && ((player.bukkitPlayer != null && player.food < 20) || player.gamemode == GameMode.CREATIVE)) {
                player.packetStateData.setSlowedByUsingItem(true);
                player.packetStateData.eatingHand = hand;
                return;
            }

            // The player cannot eat this item, resync use status
            player.packetStateData.setSlowedByUsingItem(false);
        }

        if (material == ItemTypes.SHIELD && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9)) {
            player.packetStateData.setSlowedByUsingItem(true);
            player.packetStateData.eatingHand = hand;
            return;
        }

        // Avoid releasing crossbow as being seen as slowing player
        final NBTCompound nbt = item.getNBT(); // How can this be null?
        if (material == ItemTypes.CROSSBOW && nbt != null && nbt.getBoolean("Charged")) {
            player.packetStateData.setSlowedByUsingItem(false); // TODO: Fix this
            return;
        }

        // The client and server don't agree on trident status because mojang is incompetent at netcode.
        if (material == ItemTypes.TRIDENT
                && item.getDamageValue() < item.getMaxDamage() - 1 // Player can't use item if it's "about to break"
                && (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_13_2)
                || player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8))) {
            player.packetStateData.setSlowedByUsingItem(item.getEnchantmentLevel(EnchantmentTypes.RIPTIDE, PacketEvents.getAPI().getServerManager().getVersion().toClientVersion()) <= 0);
            player.packetStateData.eatingHand = hand;
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
            // I think we may have to cancel the bukkit event if the player isn't slowed
            // On 1.8, it wouldn't be too bad to handle bows correctly
            // But on 1.9+, no idle packet and clients/servers don't agree on bow status
            // Mojang pls fix
            player.packetStateData.setSlowedByUsingItem(false);
        }

        if (material == ItemTypes.SPYGLASS && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_17)) {
            player.packetStateData.setSlowedByUsingItem(true);
            player.packetStateData.eatingHand = hand;
        }

        if (material == ItemTypes.GOAT_HORN && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_19)) {
            player.packetStateData.setSlowedByUsingItem(true);
            player.packetStateData.eatingHand = hand;
        }

        // Only 1.8 and below players can block with swords
        if (material.hasAttribute(ItemTypes.ItemAttribute.SWORD)) {
            if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8))
                player.packetStateData.setSlowedByUsingItem(true);
            else if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_9)) // ViaVersion stuff
                player.packetStateData.setSlowedByUsingItem(false);
        }
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            WrapperPlayClientPlayerDigging dig = new WrapperPlayClientPlayerDigging(event);

            if (dig.getAction() == DiggingAction.RELEASE_USE_ITEM) {
                final GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
                if (player == null) return;

                player.packetStateData.setSlowedByUsingItem(false);
                player.packetStateData.slowedByUsingItemTransaction = player.lastTransactionReceived.get();

                if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_13)) {
                    ItemStack hand = player.packetStateData.eatingHand == InteractionHand.OFF_HAND ? player.getInventory().getOffHand() : player.getInventory().getHeldItem();

                    if (hand.getType() == ItemTypes.TRIDENT
                            && hand.getEnchantmentLevel(EnchantmentTypes.RIPTIDE, PacketEvents.getAPI().getServerManager().getVersion().toClientVersion()) > 0) {
                        player.packetStateData.tryingToRiptide = true;
                    }
                }
            }
        }

        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType()) || event.getPacketType() == PacketType.Play.Client.CLIENT_TICK_END) {
            final GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player != null && player.packetStateData.isSlowedByUsingItem()
                    && !player.packetStateData.lastPacketWasTeleport
                    && !player.packetStateData.lastPacketWasOnePointSeventeenDuplicate) {
                if (player.packetStateData.eatingHand != InteractionHand.OFF_HAND
                        && player.packetStateData.getSlowedByUsingItemSlot() != player.packetStateData.lastSlotSelected
                        || player.getInventory().getItemInHand(player.packetStateData.eatingHand).isEmpty()) {
                    player.packetStateData.setSlowedByUsingItem(false);
                    player.checkManager.getPostPredictionCheck(NoSlow.class).didSlotChangeLastTick = true;
                }
            }
        }

        if (event.getPacketType() == PacketType.Play.Client.HELD_ITEM_CHANGE) {
            final int slot = new WrapperPlayClientHeldItemChange(event).getSlot();

            // Stop people from spamming the server with out of bounds exceptions
            if (slot > 8 || slot < 0) return;

            final GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;

            // do we need to do this with block breaks too?
            // Prevent issues if the player switches slots, while lagging, standing still, and is placing blocks
            CheckManagerListener.handleQueuedPlaces(player, false, 0, 0, System.currentTimeMillis());

            if (player.packetStateData.lastSlotSelected != slot) {
                // just assume they tick after this
                if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9) && !player.supportsEndTick() && !player.isTickingReliablyFor(3) && player.packetStateData.eatingHand != InteractionHand.OFF_HAND) {
                    player.packetStateData.setSlowedByUsingItem(false);
                }
            }
            player.packetStateData.lastSlotSelected = slot;
        }

        if (event.getPacketType() == PacketType.Play.Client.USE_ITEM || (event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT && new WrapperPlayClientPlayerBlockPlacement(event).getFace() == BlockFace.OTHER)) {
            final GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;

            final InteractionHand hand = event.getPacketType() == PacketType.Play.Client.USE_ITEM
                    ? new WrapperPlayClientUseItem(event).getHand()
                    : InteractionHand.MAIN_HAND;

            if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_8)
                    && player.gamemode == GameMode.SPECTATOR)
                return;

            player.packetStateData.slowedByUsingItemTransaction = player.lastTransactionReceived.get();

            final ItemStack item = hand == InteractionHand.MAIN_HAND ?
                    player.getInventory().getHeldItem() : player.getInventory().getOffHand();

            handleUseItem(player, item, hand);
        }
    }
}
