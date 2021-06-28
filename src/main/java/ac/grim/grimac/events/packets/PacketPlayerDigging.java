package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.nmsImplementations.Materials;
import ac.grim.grimac.utils.nmsImplementations.XMaterial;
import io.github.retrooper.packetevents.event.PacketListenerAbstract;
import io.github.retrooper.packetevents.event.impl.PacketPlayReceiveEvent;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.play.in.blockdig.WrappedPacketInBlockDig;
import io.github.retrooper.packetevents.packetwrappers.play.in.blockplace.WrappedPacketInBlockPlace;
import io.github.retrooper.packetevents.packetwrappers.play.in.helditemslot.WrappedPacketInHeldItemSlot;
import io.github.retrooper.packetevents.utils.player.Hand;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CrossbowMeta;

public class PacketPlayerDigging extends PacketListenerAbstract {
    Material crossbow = XMaterial.CROSSBOW.parseMaterial();
    Material trident = XMaterial.TRIDENT.parseMaterial();

    @Override
    public void onPacketPlayReceive(PacketPlayReceiveEvent event) {
        byte packetID = event.getPacketId();

        if (packetID == PacketType.Play.Client.BLOCK_DIG) {
            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());

            if (player == null) return;

            WrappedPacketInBlockDig dig = new WrappedPacketInBlockDig(event.getNMSPacket());

            WrappedPacketInBlockDig.PlayerDigType type = dig.getDigType();
            if ((type == WrappedPacketInBlockDig.PlayerDigType.DROP_ALL_ITEMS && player.packetStateData.eatingHand == Hand.MAIN_HAND) ||
                    type == WrappedPacketInBlockDig.PlayerDigType.RELEASE_USE_ITEM ||
                    type == WrappedPacketInBlockDig.PlayerDigType.SWAP_ITEM_WITH_OFFHAND) {

                player.packetStateData.isEating = false;

                if (XMaterial.supports(13)) {
                    ItemStack main = player.bukkitPlayer.getInventory().getItemInMainHand();
                    ItemStack off = player.bukkitPlayer.getInventory().getItemInOffHand();

                    int j = 0;
                    if (main.getType() == Material.TRIDENT) {
                        j = main.getEnchantmentLevel(Enchantment.RIPTIDE);
                    } else if (off.getType() == Material.TRIDENT) {
                        j = off.getEnchantmentLevel(Enchantment.RIPTIDE);
                    }

                    if (j > 0) {
                        player.packetStateData.tryingToRiptide = true;
                    }
                }
            }
        }

        if (packetID == PacketType.Play.Client.HELD_ITEM_SLOT) {
            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
            if (player == null) return;

            WrappedPacketInHeldItemSlot slot = new WrappedPacketInHeldItemSlot(event.getNMSPacket());

            // Stop people from spamming the server with out of bounds exceptions
            if (slot.getCurrentSelectedSlot() > 8) return;

            player.packetStateData.lastSlotSelected = slot.getCurrentSelectedSlot();

            if (player.packetStateData.eatingHand == Hand.MAIN_HAND) {
                player.packetStateData.isEating = false;
            }
        }

        if (packetID == PacketType.Play.Client.BLOCK_PLACE) {
            WrappedPacketInBlockPlace place = new WrappedPacketInBlockPlace(event.getNMSPacket());
            ItemStack itemStack;

            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
            if (player == null) return;

            if (place.getHand() == Hand.MAIN_HAND) {
                itemStack = player.bukkitPlayer.getInventory().getItem(player.packetStateData.lastSlotSelected);
            } else {
                itemStack = player.bukkitPlayer.getInventory().getItemInOffHand();
            }

            if (itemStack != null && Materials.isUsable(itemStack.getType())) {
                player.packetStateData.eatingHand = place.getHand();

                // Avoid releasing crossbow as being seen as slowing player
                if (itemStack.getType() == crossbow) {
                    CrossbowMeta crossbowMeta = (CrossbowMeta) itemStack.getItemMeta();
                    if (crossbowMeta != null && crossbowMeta.hasChargedProjectiles())
                        return;
                }

                player.packetStateData.isEating = true;
            }
        }
    }
}
