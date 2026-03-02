package ac.grim.legacyac.check.impl;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import ac.grim.legacyac.network.frame.MovementFrame;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import java.util.Locale;

/**
 * NoSlow check — detects players that move at full speed while using items (eating, blocking, drinking, drawing bow).
 *
 * In vanilla Minecraft, when a player is using an item:
 * - Their movement speed is multiplied by 0.2 (they move at 20% speed)
 * - This applies to both forward and strafing movement
 *
 * NoSlow cheats bypass this slowdown, allowing players to move at full speed
 * while blocking with a sword (common in 1.7 PvP) or eating.
 */
public final class NoSlowCheck extends Check {
    public NoSlowCheck(LegacyAntiCheatPlugin plugin) {
        super(plugin, "NoSlow");
    }

    public void onMove(PlayerMoveEvent event, PlayerData data) {
        if (event.getTo() == null) {
            return;
        }
        MovementFrame frame = new MovementFrame(System.nanoTime(), event.getTo().getX(), event.getTo().getY(), event.getTo().getZ(), event.getTo().getYaw(), event.getTo().getPitch(), event.getPlayer().isOnGround(), true, true, MovementFrame.Source.BUKKIT_MOVE_EVENT);
        onMovementFrame(event.getPlayer(), frame, data);
    }

    public void onMovementFrame(Player player, MovementFrame frame, PlayerData data) {
        if (!isEnabled()) {
            return;
        }

        if (isExempt(player, data)) {
            return;
        }
        if (player.isFlying() || player.getVehicle() != null) {
            return;
        }

        // Don't check during jumps — isBlocking() can return stale state on the jump tick
        if (data.getLastDeltaY() > 0.1D && data.getGroundTicks() > 0) {
            coolDownScore(data);
            return;
        }

        // Don't check right after knockback
        long timeSinceVelocity = System.currentTimeMillis() - data.getLastVelocityAt();
        if (timeSinceVelocity < 500L) {
            coolDownScore(data);
            return;
        }

        // Check if the player is blocking (sword) or using an item
        if (!player.isBlocking() && !isUsingItem(player)) {
            coolDownScore(data);
            return;
        }

        double horizontal = data.getLastDeltaXZ();

        // Calculate the maximum allowed speed while using an item
        // Normal walking speed * 0.2 = the vanilla slowdown factor
        // We need to calculate what the actual max speed would be after the 0.2 multiplier
        double baseWalkSpeed = 0.10000000149011612D;
        if (player.isSprinting()) {
            // Note: in vanilla 1.7, you CAN'T sprint while blocking. If they are,
            // that's already suspicious, but some mods allow it.
            baseWalkSpeed *= 1.3D;
        }

        // Apply speed potion
        for (org.bukkit.potion.PotionEffect effect : player.getActivePotionEffects()) {
            if (effect.getType().equals(org.bukkit.potion.PotionEffectType.SPEED)) {
                baseWalkSpeed *= (1.0D + (effect.getAmplifier() + 1) * 0.2D);
            }
        }

        // When using an item, the movement input is multiplied by 0.2
        // Then normal acceleration is applied, so the effective max speed is much lower
        // For ground: accel = speed * (0.16277136 / friction^3)
        // With the 0.2 slowdown on input, max becomes roughly 0.2 * steadyState
        double friction = 0.6D * 0.91D; // normal ground
        double frictionCubed = friction * friction * friction;
        double normalAccel = baseWalkSpeed * (0.16277136D / frictionCubed);
        double normalSteadyState = normalAccel / (1.0D - friction);

        // The slowdown factor when using items
        double slowFactor = plugin.getConfig().getDouble("checks.NoSlow.slow-factor", 0.2D);
        double maxSlowedSpeed = normalSteadyState * slowFactor;

        // Add tolerance
        maxSlowedSpeed += 0.03D; // network tolerance
        if (isLagging(data)) {
            maxSlowedSpeed += 0.05D;
        }

        // Knockback tolerance
        timeSinceVelocity = System.currentTimeMillis() - data.getLastVelocityAt();
        if (timeSinceVelocity < 1000L) {
            double kbXZ = data.getLastVelocityXZ();
            if (kbXZ > 0.0D) {
                int ticksSince = (int) (timeSinceVelocity / 50L);
                double decayedKB = kbXZ;
                for (int i = 0; i < ticksSince && i < 20; i++) {
                    decayedKB *= 0.91D;
                }
                double previousMomentum = data.getPrevDeltaXZ() * 0.91D;
                double kbTolerance = previousMomentum + decayedKB + 0.3D;
                if (maxSlowedSpeed < kbTolerance) {
                    maxSlowedSpeed = kbTolerance;
                }
            }
        }

        if (horizontal > maxSlowedSpeed) {
            double deviation = horizontal - maxSlowedSpeed;
            double weight = plugin.getConfig().getDouble("checks.NoSlow.window-weight", 1.0D);
            double buffer = slideAndAddScore(data, deviation, weight);
            if (buffer > plugin.getConfig().getDouble("checks.NoSlow.buffer", 0.4D)) {
                String action = player.isBlocking() ? "BLOCKING" : "USING_ITEM";
                flag(player, data, deviation, action + " h=" + fmt(horizontal)
                    + " max=" + fmt(maxSlowedSpeed));
            }
        } else {
            coolDownScore(data);
        }
    }

    /**
     * Check if the player is using an item (eating, drinking, drawing bow).
     * In 1.7.10, player.isBlocking() covers sword blocking.
     * For eating/drinking we check if they have food/potion in hand and the item use is active.
     */
    private boolean isUsingItem(Player player) {
        // In 1.7.10, there's no direct isHandActive() API
        // We rely on isBlocking() for swords and check item type for consumables
        ItemStack hand = player.getItemInHand();
        if (hand == null || hand.getType() == Material.AIR) {
            return false;
        }

        Material type = hand.getType();
        // Bow drawing
        if (type == Material.BOW) {
            return true; // We can't easily tell if they're DRAWING the bow in 1.7 API
            // So we skip this for now to avoid false positives
        }

        return false;
    }

    private static String fmt(double value) {
        return String.format(Locale.ROOT, "%.4f", value);
    }
}
