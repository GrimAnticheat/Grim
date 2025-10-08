package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.checks.impl.badpackets.BadPacketsW;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import ac.grim.grimac.utils.data.packetentity.PacketEntityHorse;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.enchantment.type.EnchantmentTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;

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
            if (!player.compensatedEntities.entityMap.containsKey(interact.getEntityId()) && !player.compensatedEntities.serverPositionsMap.containsKey(interact.getEntityId())
                    // the list of entities used to raytrace isn't the same as the list of entities in the world in pre-1.14 (wtf mojang)
                    && (!player.compensatedEntities.entitiesRemovedThisTick.contains(interact.getEntityId()) || player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_14))) {
                final BadPacketsW badPacketsW = player.checkManager.getCheck(BadPacketsW.class);
                if (badPacketsW.flagAndAlert("entityId=" + interact.getEntityId()) && badPacketsW.shouldModifyPackets()) {
                    event.setCancelled(true);
                    player.onPacketCancel();
                }
                return;
            }

            if (interact.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                if (player.isResetItemUsageOnAttack()) {
                    GrimAPI.INSTANCE.getItemResetHandler().resetItemUsage(player.platformPlayer);
                }

                // This is not vanilla behaviour as the attack damage attribute is marked as not synced to the client
                // However, plugins can still set this by sending an attributes packet
                if (player.compensatedEntities.self.getAttributeValue(Attributes.ATTACK_DAMAGE) <= 0) return;

                ItemStack heldItem = player.inventory.getHeldItem();
                PacketEntity entity = player.compensatedEntities.getEntity(interact.getEntityId());

                if (entity != null && (!entity.isLivingEntity || entity.type == EntityTypes.PLAYER || entity.type == EntityTypes.PAINTING
                        || entity.type == EntityTypes.ENDER_DRAGON && player.getClientVersion().isOlderThan(ClientVersion.V_1_21_2))) {
                    int knockbackLevel = player.getClientVersion().isOlderThan(ClientVersion.V_1_21) && heldItem != null
                            ? heldItem.getEnchantmentLevel(EnchantmentTypes.KNOCKBACK)
                            : 0;
                    final boolean hasNegativeKB = knockbackLevel < 0;

                    final boolean isLegacyPlayer = player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8);
                    // assume cooldown is full on 1.8 servers
                    final boolean noCooldown = isLegacyPlayer || PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_9);

                    if (!isLegacyPlayer) {
                        knockbackLevel = Math.max(knockbackLevel, 0);
                    }

                    // 1.8 players who are packet sprinting WILL get slowed
                    // 1.9+ players who are packet sprinting might not, based on attack cooldown
                    // Players with knockback enchantments always get slowed

                    if ((player.lastSprinting && !hasNegativeKB && noCooldown) || knockbackLevel > 0) {
                        player.minAttackSlow++;
                        player.maxAttackSlow++;

                        // Players cannot slow themselves twice in one tick without a knockback sword
                        if (knockbackLevel == 0) {
                            player.maxAttackSlow = player.minAttackSlow = 1;
                        }
                    } else if (!isLegacyPlayer && player.lastSprinting) {
                        // 1.9+ players who have attack speed cannot slow themselves twice in one tick because their attack cooldown gets reset on swing.
                        if (player.maxAttackSlow > 0
                                && PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_9)
                                && player.compensatedEntities.self.getAttributeValue(Attributes.ATTACK_SPEED) < 16) { // 16 is a reasonable limit
                            return;
                        }

                        // 1.9+ player who might have been slowed, but we can't be sure
                        player.maxAttackSlow++;
                    }
                }
            } else if (interact.getAction() == WrapperPlayClientInteractEntity.InteractAction.INTERACT) {
                // Interacting with a horse in versions 1.13- will cause the client to
                // set the player's rotation to the horse's rotation
                if (player.compensatedEntities.getEntity(interact.getEntityId()) instanceof PacketEntityHorse
                        && player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_13)) {
                    player.packetStateData.horseInteractCausedForcedRotation = true;
                }
            }
        }
    }
}
