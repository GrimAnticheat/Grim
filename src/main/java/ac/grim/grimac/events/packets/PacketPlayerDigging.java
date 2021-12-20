package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.AlmostBoolean;
import ac.grim.grimac.utils.nmsutil.XMaterial;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.impl.PacketReceiveEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientHeldItemChange;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUseItem;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.CrossbowMeta;

public class PacketPlayerDigging extends PacketListenerAbstract {

    private static final Material CROSSBOW = XMaterial.CROSSBOW.parseMaterial();
    private static final Material BOW = XMaterial.BOW.parseMaterial();
    private static final Material TRIDENT = XMaterial.TRIDENT.parseMaterial();
    private static final Material SHIELD = XMaterial.SHIELD.parseMaterial();

    private static final Material ARROW = XMaterial.ARROW.parseMaterial();
    private static final Material TIPPED_ARROW = XMaterial.TIPPED_ARROW.parseMaterial();
    private static final Material SPECTRAL_ARROW = XMaterial.SPECTRAL_ARROW.parseMaterial();

    private static final Material POTION = XMaterial.POTION.parseMaterial();
    private static final Material MILK_BUCKET = XMaterial.MILK_BUCKET.parseMaterial();

    private static final Material GOLDEN_APPLE = XMaterial.GOLDEN_APPLE.parseMaterial();
    private static final Material ENCHANTED_GOLDEN_APPLE = XMaterial.ENCHANTED_GOLDEN_APPLE.parseMaterial();
    private static final Material HONEY_BOTTLE = XMaterial.HONEY_BOTTLE.parseMaterial();

    public PacketPlayerDigging() {
        super(PacketListenerPriority.LOW);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {

            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer((Player) event.getPlayer());
            if (player == null) return;

            WrapperPlayClientPlayerDigging dig = new WrapperPlayClientPlayerDigging(event);

            if (dig.getAction() == WrapperPlayClientPlayerDigging.Action.RELEASE_USE_ITEM) {
                player.packetStateData.slowedByUsingItem = AlmostBoolean.FALSE;
                player.packetStateData.slowedByUsingItemTransaction = player.lastTransactionReceived.get();

                if (XMaterial.supports(13)) {
                    org.bukkit.inventory.ItemStack main = player.bukkitPlayer.getInventory().getItemInMainHand();
                    org.bukkit.inventory.ItemStack off = player.bukkitPlayer.getInventory().getItemInOffHand();

                    int j = 0;
                    if (main.getType() == TRIDENT) {
                        j = main.getEnchantmentLevel(Enchantment.RIPTIDE);
                    } else if (off.getType() == TRIDENT) {
                        j = off.getEnchantmentLevel(Enchantment.RIPTIDE);
                    }

                    if (j > 0) {
                        // TODO: Re-add riptide support
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
            WrapperPlayClientUseItem place = new WrapperPlayClientUseItem(event);

            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer((Player) event.getPlayer());
            if (player == null) return;

            if (XMaterial.supports(8) && player.gamemode == GameMode.SPECTATOR)
                return;

            // This was an interaction with a block, not a use item
            if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_9) && place.getFace() != BlockFace.OTHER)
                return;

            player.packetStateData.slowedByUsingItemTransaction = player.lastTransactionReceived.get();

            // Design inspired by NoCheatPlus, but rewritten to be faster
            // https://github.com/Updated-NoCheatPlus/NoCheatPlus/blob/master/NCPCompatProtocolLib/src/main/java/fr/neatmonster/nocheatplus/checks/net/protocollib/NoSlow.java
            org.bukkit.inventory.ItemStack item = place.getHand() == InteractionHand.MAIN_HAND ? player.bukkitPlayer.getInventory().getItem(player.packetStateData.lastSlotSelected) : player.bukkitPlayer.getInventory().getItemInOffHand();
            if (item != null) {
                Material material = item.getType();

                if (player.checkManager.getCompensatedCooldown().hasMaterial(material)) {
                    player.packetStateData.slowedByUsingItem = AlmostBoolean.FALSE; // resync, not required
                    return; // The player has a cooldown, and therefore cannot use this item!
                }

                // 1.14 and below players cannot eat in creative, exceptions are potions or milk
                if ((player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_15) ||
                        player.gamemode != GameMode.CREATIVE && material.isEdible())
                        || material == POTION || material == MILK_BUCKET) {
                    // pre1.9 splash potion
                    if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_8_8) && item.getDurability() > 16384)
                        return;

                    // Eatable items that don't require any hunger to eat
                    if (material == Material.POTION || material == Material.MILK_BUCKET
                            || material == GOLDEN_APPLE || material == ENCHANTED_GOLDEN_APPLE || material == HONEY_BOTTLE) {
                        player.packetStateData.slowedByUsingItem = AlmostBoolean.TRUE;
                        player.packetStateData.eatingHand = place.getHand();

                        return;
                    }

                    // The other items that do require it
                    // TODO: Food level lag compensation
                    if (item.getType().isEdible() && (((Player) event.getPlayer()).getFoodLevel() < 20 || player.gamemode == GameMode.CREATIVE)) {
                        player.packetStateData.slowedByUsingItem = AlmostBoolean.TRUE;
                        player.packetStateData.eatingHand = place.getHand();

                        return;
                    }

                    // The player cannot eat this item, resync use status
                    player.packetStateData.slowedByUsingItem = AlmostBoolean.FALSE;
                }

                if (material == SHIELD) {
                    player.packetStateData.slowedByUsingItem = AlmostBoolean.TRUE;
                    player.packetStateData.eatingHand = place.getHand();

                    return;
                }

                // Avoid releasing crossbow as being seen as slowing player
                if (material == CROSSBOW) {
                    CrossbowMeta crossbowMeta = (CrossbowMeta) item.getItemMeta();
                    if (crossbowMeta != null && crossbowMeta.hasChargedProjectiles())
                        return;
                }

                // The client and server don't agree on trident status because mojang is incompetent at netcode.
                if (material == TRIDENT) {
                    if (item.getEnchantmentLevel(Enchantment.RIPTIDE) > 0)
                        player.packetStateData.slowedByUsingItem = AlmostBoolean.MAYBE;
                    else
                        player.packetStateData.slowedByUsingItem = AlmostBoolean.TRUE;
                    player.packetStateData.eatingHand = place.getHand();
                }

                // Players in survival can't use a bow without an arrow
                // Crossbow charge checked previously
                if (material == BOW || material == CROSSBOW) {
                    player.packetStateData.slowedByUsingItem = (player.gamemode == GameMode.CREATIVE ||
                            hasItem(player, ARROW) || hasItem(player, TIPPED_ARROW) || hasItem(player, SPECTRAL_ARROW)) ? AlmostBoolean.TRUE : AlmostBoolean.FALSE;
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

    private boolean hasItem(GrimPlayer player, Material material) {
        return material != null && player.bukkitPlayer.getInventory().contains(material)
                || (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_9) && (player.bukkitPlayer.getInventory().getItemInOffHand().getType() == ARROW
                || player.bukkitPlayer.getInventory().getItemInOffHand().getType() == TIPPED_ARROW
                || player.bukkitPlayer.getInventory().getItemInOffHand().getType() == SPECTRAL_ARROW));
    }
}
