package ac.grim.legacyac.check.impl;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import ac.grim.legacyac.network.frame.MovementFrame;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import java.util.Locale;

/**
 * Speed check using correct Minecraft 1.7.10 physics-based speed limits.
 *
 * Key fix: the sprint-jump horizontal boost (+0.2) is only applied when the
 * player actually transitions from ground to air (deltaY > 0 && wasOnGround),
 * not on every ground tick — the previous version added it always which made
 * the max ~0.50 instead of ~0.29 for normal sprinting.
 */
public final class SpeedCheck extends Check {
    public SpeedCheck(LegacyAntiCheatPlugin plugin) {
        super(plugin, "Speed");
    }

    public void onMovementFrame(Player player, MovementFrame frame, Location from, Location to, PlayerData data) {
        if (!isEnabled()) {
            return;
        }

        if (isExempt(player, data)) {
            return;
        }
        if (player.isFlying() || player.getVehicle() != null) {
            return;
        }

        double horizontal = data.getLastDeltaXZ();
        double deltaY = data.getLastDeltaY();
        boolean onGround = frame.isOnGround();
        boolean wasOnGround = data.wasOnGround();
        PlayerData.MovementStateSnapshot state = data.getMovementStateSnapshot();

        // ---- Calculate physics-based max speed ----

        // Determine friction
        Block belowBlock = to.clone().add(0.0D, -1.0D, 0.0D).getBlock();
        Material belowType = belowBlock.getType();
        double slipperiness = getBlockSlipperiness(belowType);
        double friction;
        if (onGround) {
            friction = slipperiness * 0.91D;
        } else {
            friction = 0.91D;
        }

        // Sprint-jump detection: need to know if jumping BEFORE sprint state check
        boolean isJumping = deltaY > 0.1D && !onGround && wasOnGround;

        // Determine sprint state — on jump tick, also check previous tick's sprint
        // because 1.7.10 packet ordering can desync sprint and jump by 1 tick
        boolean sprinting = player.isSprinting() || (isJumping && data.wasSprinting());

        // Calculate acceleration
        double attributeSpeed = 0.10000000149011612D; // default movement speed
        if (sprinting) {
            attributeSpeed *= 1.3D;
        }

        // Speed potion
        PotionEffect speed = getPotion(player, PotionEffectType.SPEED);
        if (speed != null) {
            attributeSpeed *= (1.0D + (speed.getAmplifier() + 1) * 0.2D);
        }

        // Slowness potion
        PotionEffect slow = getPotion(player, PotionEffectType.SLOW);
        if (slow != null) {
            attributeSpeed *= Math.max(0.0D, 1.0D - (slow.getAmplifier() + 1) * 0.15D);
        }

        double acceleration;
        if (onGround) {
            double frictionCubed = friction * friction * friction;
            acceleration = attributeSpeed * (0.16277136D / frictionCubed);
        } else {
            acceleration = sprinting ? 0.026D : 0.02D;
        }

        // Steady-state max speed: acceleration / (1 - friction)
        double steadyStateMax = acceleration / (1.0D - friction);

        // burstMax: carried velocity from PREVIOUS tick + new acceleration
        // Note: data.getLastDeltaXZ() is the CURRENT tick's value,
        // getPrevDeltaXZ() is the PREVIOUS tick's value which represents carried velocity
        double burstMax = data.getPrevDeltaXZ() * friction + acceleration;
        // When landing (transitioning from air to ground), the velocity was previously under
        // air friction (0.91) but now uses ground friction (0.546). This transition takes
        // 2-3 ticks to settle. Use air friction for burst during this window.
        if (onGround && data.getGroundTicks() <= 3) {
            double airBurst = data.getPrevDeltaXZ() * 0.91D + acceleration;
            burstMax = Math.max(burstMax, airBurst);
        }
        double max = Math.max(steadyStateMax, burstMax);

        // Sprint-jump horizontal boost: ONLY when player is jumping this tick
        // (transitioning from ground to air with upward Y)
        if (isJumping && sprinting) {
            max += 0.2D;
        }

        // Special blocks
        Material feetType = to.getBlock().getType();
        if (feetType == Material.WEB || belowType == Material.WEB) {
            max = Math.min(max, 0.12D);
        }
        if (feetType == Material.SOUL_SAND) {
            max *= 0.6D;
        }
        if (isLiquid(feetType) || isLiquid(belowType)) {
            max = Math.max(max, 0.16D);
        }

        // Ice: allow higher steady-state due to accumulated velocity
        if (isIce(belowType)) {
            max = Math.max(max, burstMax + 0.04D);
        }

        // Movement threshold tolerance
        max += 0.005D;

        // Network/timing tolerance
        max += 0.01D;

        // Adaptive lag: prioritize state alignment, only add small tolerance once aligned
        double baseMax = max;
        if (isLagging(data) && state.isFullyAligned()) {
            max += plugin.getConfig().getDouble("adaptive-lag.speed-small-margin", 0.03D);
        }
        if (!state.isFullyAligned()) {
            max += plugin.getConfig().getDouble("adaptive-lag.pending-state-margin", 0.06D);
        }
        logAdaptiveLagComparison(player, data, getName(), baseMax, max, "speed-state-aligned=" + state.isFullyAligned());

        // Knockback tolerance — knockback creates an instantaneous velocity injection that
        // the burst model (prev*friction+accel) cannot account for because prev was pre-knockback.
        // Use the ACTUAL stored knockback velocity to calculate expected max speed.
        long timeSinceVelocity = System.currentTimeMillis() - data.getLastVelocityAt();
        if (timeSinceVelocity < 1000L) {
            double kbXZ = data.getLastVelocityXZ();
            if (kbXZ > 0.0D) {
                // Knockback applies the velocity directly. Over time it decays via friction.
                // Calculate what the speed should be after timeSinceVelocity ms of friction decay.
                // Each tick = 50ms, friction per tick = 0.91 (air) or ~0.546 (ground)
                int ticksSince = (int) (timeSinceVelocity / 50L);
                double decayedKB = kbXZ;
                for (int i = 0; i < ticksSince && i < 20; i++) {
                    decayedKB *= 0.91D; // conservative: use air friction (slower decay)
                }
                // The allowed max should be at least the decayed knockback speed + existing momentum + tolerance
                // During a tick where the client processes a velocity packet, it can combine the
                // knockback velocity with its existing momentum and sprint effects.
                double kbMax = burstMax + decayedKB + 0.3D; 
                if (kbMax > max) {
                    max = kbMax;
                }
            }
        }

        // Debug: show computed values for debugging
        if (data.isDebugEnabled()) {
            plugin.getLogger().info("[GLAC-DEBUG] " + player.getName() + " Speed h="
                + fmt(horizontal) + " max=" + fmt(max) + " prev=" + fmt(data.getPrevDeltaXZ())
                + " accel=" + fmt(acceleration) + " steady=" + fmt(steadyStateMax)
                + " burst=" + fmt(burstMax) + " onGround=" + onGround
                + " sprint=" + sprinting + " jump=" + isJumping
                + " pending=" + state.getPendingChanges());
        }

        // ---- Check and flag ----
        if (horizontal > max) {
            double deviation = horizontal - max;
            double weight = plugin.getConfig().getDouble("checks.Speed.window-weight", 1.0D);
            double buffer = slideAndAddScore(data, deviation, weight);
            if (buffer > plugin.getConfig().getDouble("checks.Speed.buffer", 0.35D)) {
                flag(player, data, deviation, "h=" + fmt(horizontal) + " max=" + fmt(max)
                    + " spd=" + fmt(attributeSpeed) + " jump=" + isJumping);
            }
        } else {
            coolDownScore(data);
            if (onGround && from.getY() == to.getY()) {
                data.setLastSafeLocation(to.clone());
            }
        }
    }

    private static double getBlockSlipperiness(Material material) {
        if (material == Material.ICE || material == Material.PACKED_ICE) {
            return 0.98D;
        }
        return 0.6D;
    }

    private static boolean isIce(Material material) {
        return material == Material.ICE || material == Material.PACKED_ICE;
    }

    private static boolean isLiquid(Material material) {
        return material == Material.WATER || material == Material.STATIONARY_WATER
            || material == Material.LAVA || material == Material.STATIONARY_LAVA;
    }

    private PotionEffect getPotion(Player player, PotionEffectType type) {
        for (PotionEffect effect : player.getActivePotionEffects()) {
            if (effect.getType().equals(type)) {
                return effect;
            }
        }
        return null;
    }

    private String fmt(double value) {
        return String.format(Locale.ROOT, "%.4f", value);
    }
}
