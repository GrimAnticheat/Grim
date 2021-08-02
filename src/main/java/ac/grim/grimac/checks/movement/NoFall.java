package ac.grim.grimac.checks.movement;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.math.GrimMathHelper;
import ac.grim.grimac.utils.nmsImplementations.Materials;
import ac.grim.grimac.utils.nmsImplementations.XMaterial;
import io.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.packetwrappers.play.out.animation.WrappedPacketOutAnimation;
import io.github.retrooper.packetevents.utils.server.ServerVersion;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.EntityEffect;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class NoFall {
    private static final Material SLIME_BLOCK = XMaterial.SLIME_BLOCK.parseMaterial();
    private static final Material HONEY_BLOCK = XMaterial.HONEY_BLOCK.parseMaterial();
    private static final Material HAY_BALE = XMaterial.HAY_BLOCK.parseMaterial();

    private boolean playerUsedNoFall = false;

    public void tickNoFall(GrimPlayer player, Material onBlock, Vector collide) {
        // Catch players claiming to be on the ground when they actually aren't
        // Catch players claiming to be off the ground when they actually are
        //
        // Catch players changing their ground status with a ground packet
        if (player.isActuallyOnGround != player.onGround || (player.uncertaintyHandler.didGroundStatusChangeWithoutPositionPacket && !player.uncertaintyHandler.lastTickWasNearGroundZeroPointZeroThree)) {
            playerUsedNoFall = true;
        }

        if (player.fallDistance == 0)
            playerUsedNoFall = false;

        if (player.bukkitPlayer.getGameMode().getValue() == 1 || player.bukkitPlayer.getGameMode().getValue() == 3) {
            playerUsedNoFall = false;
            return;
        }

        if (player.isActuallyOnGround) {
            if (player.fallDistance > 0) {
                // Bed multiplier is 0.5 - 1.12+
                // Hay multiplier is 0.2 - 1.9+
                // Honey multiplier is 0.2 - 1.15+
                // Slime multiplier is 0 - all versions
                float blockFallDamageMultiplier = 1;

                if (Materials.checkFlag(onBlock, Materials.BED) && ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_12)) {
                    blockFallDamageMultiplier = 0.5f;
                } else if (onBlock == HAY_BALE && ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_9)) {
                    blockFallDamageMultiplier = 0.2f;
                } else if (onBlock == HONEY_BLOCK) {
                    blockFallDamageMultiplier = 0.2f;
                } else if (onBlock == SLIME_BLOCK && !player.isSneaking) {
                    blockFallDamageMultiplier = 0;
                }

                double damage = Math.max(0, Math.ceil((player.fallDistance - 3.0F - player.jumpAmplifier) * blockFallDamageMultiplier));

                ItemStack boots = player.bukkitPlayer.getInventory().getBoots();
                ItemStack leggings = player.bukkitPlayer.getInventory().getLeggings();
                ItemStack chestplate = player.bukkitPlayer.getInventory().getChestplate();
                ItemStack helmet = player.bukkitPlayer.getInventory().getHelmet();

                if (damage > 0.0) {
                    int damagePercentTaken = 100;
                    // Each level of feather falling reduces damage by 48%
                    // Each level of protection reduces damage by 4%
                    // This can stack up to a total of 80% damage reduction
                    if (boots != null) {
                        damagePercentTaken -= boots.getEnchantmentLevel(Enchantment.PROTECTION_FALL) * 12;
                        damagePercentTaken -= boots.getEnchantmentLevel(Enchantment.PROTECTION_ENVIRONMENTAL) * 4;
                    }

                    if (leggings != null)
                        damagePercentTaken -= leggings.getEnchantmentLevel(Enchantment.PROTECTION_ENVIRONMENTAL) * 4;

                    if (chestplate != null)
                        damagePercentTaken -= chestplate.getEnchantmentLevel(Enchantment.PROTECTION_ENVIRONMENTAL) * 4;

                    if (helmet != null)
                        damagePercentTaken -= helmet.getEnchantmentLevel(Enchantment.PROTECTION_ENVIRONMENTAL) * 4;

                    if (damagePercentTaken < 100) {
                        damagePercentTaken = Math.max(damagePercentTaken, 20);
                        damage = (int) (damage * damagePercentTaken / 100);
                    }
                }

                if (playerUsedNoFall && damage > 0) {
                    float finalBlockFallDamageMultiplier = blockFallDamageMultiplier;

                    double finalDamage = damage;
                    Bukkit.getScheduler().runTask(GrimAC.plugin, () -> {
                        EntityDamageEvent fallDamage = new EntityDamageEvent(player.bukkitPlayer, EntityDamageEvent.DamageCause.FALL, finalBlockFallDamageMultiplier);
                        Bukkit.getServer().getPluginManager().callEvent(fallDamage);
                        // Future versions could play the hurt sound and the animation
                        if (!fallDamage.isCancelled()) {
                            player.bukkitPlayer.setLastDamageCause(fallDamage);
                            player.bukkitPlayer.playEffect(EntityEffect.HURT);
                            PacketEvents.get().getPlayerUtils().sendPacket(player.bukkitPlayer, new WrappedPacketOutAnimation(player.entityID, WrappedPacketOutAnimation.EntityAnimationType.TAKE_DAMAGE));
                            player.bukkitPlayer.setHealth(GrimMathHelper.clamp(player.bukkitPlayer.getHealth() - finalDamage, 0, player.bukkitPlayer.getMaxHealth()));
                            Bukkit.broadcastMessage(ChatColor.RED + "" + player.bukkitPlayer.getName() + " used nofall so we are applying fall damage");
                        }
                    });
                }

                player.fallDistance = 0;
            }
        } else if (collide.getY() < 0) {
            player.fallDistance -= collide.getY();
        }
    }
}
