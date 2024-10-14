package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.checks.impl.badpackets.BadPacketsW;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.enchantment.type.EnchantmentTypes;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

import static ac.grim.grimac.utils.inventory.Inventory.HOTBAR_OFFSET;

public class PacketPlayerAttack extends PacketListenerAbstract {

    public PacketPlayerAttack() {
        super(PacketListenerPriority.LOW);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity interact = new WrapperPlayClientInteractEntity(event);
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());

            if (player == null) return;

            // The entity does not exist
            if (!player.compensatedEntities.entityMap.containsKey(interact.getEntityId()) && !player.compensatedEntities.serverPositionsMap.containsKey(interact.getEntityId())) {
                if (player.checkManager.getPacketCheck(BadPacketsW.class).flagAndAlert("entityId=" + interact.getEntityId()) && player.checkManager.getPacketCheck(BadPacketsW.class).shouldModifyPackets()) {
                    event.setCancelled(true);
                    player.onPacketCancel();
                }
                return;
            }

            if (interact.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                if (GrimAPI.INSTANCE.getConfigManager().isMitigateAutoBlock()) {
                    player.resetBukkitItemUsage();
                }

                ItemStack heldItem = player.getInventory().getHeldItem();
                PacketEntity entity = player.compensatedEntities.getEntity(interact.getEntityId());

                // You don't get a release use item with block hitting with a sword?
                if (player.getClientVersion().isOlderThan(ClientVersion.V_1_9) && player.packetStateData.isSlowedByUsingItem()) {
                    ItemStack item = player.getInventory().inventory.getPlayerInventoryItem(player.packetStateData.getSlowedByUsingItemSlot() + HOTBAR_OFFSET);
                    if (item.getType().hasAttribute(ItemTypes.ItemAttribute.SWORD)) {
                        player.packetStateData.setSlowedByUsingItem(false);
                    }
                }

                if (entity != null && (!(entity.isLivingEntity()) || entity.getType() == EntityTypes.PLAYER)) {
                    boolean hasKnockbackSword = heldItem != null && heldItem.getEnchantmentLevel(EnchantmentTypes.KNOCKBACK, PacketEvents.getAPI().getServerManager().getVersion().toClientVersion()) > 0;
                    boolean isLegacyPlayer = player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8);
                    boolean hasNegativeKB = heldItem != null && heldItem.getEnchantmentLevel(EnchantmentTypes.KNOCKBACK, PacketEvents.getAPI().getServerManager().getVersion().toClientVersion()) < 0;

                    // 1.8 players who are packet sprinting WILL get slowed
                    // 1.9+ players who are packet sprinting might not, based on attack cooldown
                    // Players with knockback enchantments always get slowed
                    if ((player.isSprinting && !hasNegativeKB && isLegacyPlayer) || hasKnockbackSword) {
                        player.minPlayerAttackSlow += 1;
                        player.maxPlayerAttackSlow += 1;

                        // Players cannot slow themselves twice in one tick without a knockback sword
                        if (!hasKnockbackSword) {
                            player.minPlayerAttackSlow = 0;
                            player.maxPlayerAttackSlow = 1;
                        }
                    } else if (!isLegacyPlayer && player.isSprinting) {
                        // 1.9+ player who might have been slowed, but we can't be sure
                        player.maxPlayerAttackSlow += 1;
                    }
                }
            }
        }

        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;

            player.minPlayerAttackSlow = 0;
        }
    }
}
