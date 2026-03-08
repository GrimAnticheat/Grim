package ac.grim.legacyac.check.impl;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import ac.grim.legacyac.network.frame.MovementFrame;
import ac.grim.legacyac.tolerance.ToleranceBudgetEngine;
import java.util.Locale;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

public final class NoSlowCheck extends Check {
    public NoSlowCheck(LegacyAntiCheatPlugin plugin) {
        super(plugin, "NoSlow");
    }

    public void onMove(PlayerMoveEvent event, PlayerData data) {
        if (event.getTo() == null) {
            return;
        }
        MovementFrame frame = new MovementFrame(System.nanoTime(), event.getTo().getX(), event.getTo().getY(),
                event.getTo().getZ(), event.getTo().getYaw(), event.getTo().getPitch(), event.getPlayer().isOnGround(),
                true, true, MovementFrame.Source.BUKKIT_MOVE_EVENT);
        onMovementFrame(event.getPlayer(), frame, data);
    }

    public void onMovementFrame(Player player, MovementFrame frame, PlayerData data) {
        if (!isEnabled() || isExempt(player, data) || player.isFlying() || player.getVehicle() != null) {
            return;
        }

        boolean usingItemCandidate = player.isBlocking() || isUsingItem(player, data);
        data.updateUsingItemSignal(usingItemCandidate);

        int minUsingTicks = plugin.getConfig().getInt("checks.NoSlow.min-using-ticks", 2);
        if (data.getTicksUsingItem() < minUsingTicks) {
            coolDownScore(data);
            data.resetNoSlowViolationStreak();
            return;
        }

        if (isGraceWindow(data)) {
            coolDownScore(data);
            data.resetNoSlowViolationStreak();
            return;
        }

        double horizontal = data.getLastDeltaXZ();
        double baseWalkSpeed = 0.10000000149011612D;
        try {
            float walkSpeed = player.getWalkSpeed();
            if (walkSpeed > 0.0F) {
                baseWalkSpeed *= (walkSpeed / 0.2F);
            }
        } catch (Throwable ignored) {
        }
        if (player.isSprinting()) {
            baseWalkSpeed *= 1.3D;
        }
        for (org.bukkit.potion.PotionEffect effect : player.getActivePotionEffects()) {
            if (effect.getType().equals(org.bukkit.potion.PotionEffectType.SPEED)) {
                baseWalkSpeed *= (1.0D + (effect.getAmplifier() + 1) * 0.2D);
            }
        }

        double friction = 0.6D * 0.91D;
        double frictionCubed = friction * friction * friction;
        double normalAccel = baseWalkSpeed * (0.16277136D / frictionCubed);
        double normalSteadyState = normalAccel / (1.0D - friction);
        double slowFactor = plugin.getConfig().getDouble("checks.NoSlow.slow-factor", 0.2D);
        double maxSlowedSpeed = normalSteadyState * slowFactor;

        ToleranceBudgetEngine.BudgetSnapshot budget = getBudget(data);
        if (budget != null) {
            maxSlowedSpeed += budget.getMovementAllowance();
        } else {
            maxSlowedSpeed += 0.03D;
            if (isLagging(data)) {
                maxSlowedSpeed += 0.05D;
            }
        }
        if (player.isBlocking()) {
            maxSlowedSpeed += 0.02D;
        }

        long timeSinceVelocity = System.currentTimeMillis() - data.getLastVelocityAt();
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
            double predictionMinDeviation = data.getPredictionReducedDeviation();
            double predictionThreshold = plugin.getConfig().getDouble("checks.NoSlow.prediction-min-deviation-threshold", 0.035D);
            if (predictionMinDeviation <= predictionThreshold && deviation < 0.055D) {
                coolDownScore(data);
                data.resetNoSlowViolationStreak();
                return;
            }

            int streak = data.incrementNoSlowViolationStreak();
            int minViolationTicks = plugin.getConfig().getInt("checks.NoSlow.min-consecutive-violation-ticks", 2);
            if (streak < minViolationTicks) {
                coolDownScore(data);
                return;
            }

            double weight = plugin.getConfig().getDouble("checks.NoSlow.window-weight", 1.0D);
            double buffer = slideAndAddScore(data, deviation, weight);
            if (buffer > plugin.getConfig().getDouble("checks.NoSlow.buffer", 0.4D)) {
                String action = player.isBlocking() ? "BLOCKING" : "USING_ITEM";
                flag(player, data, deviation, action + " h=" + fmt(horizontal)
                        + " max=" + fmt(maxSlowedSpeed)
                        + " conf=" + fmt(data.getUsingItemConfidence())
                        + " useTicks=" + data.getTicksUsingItem()
                        + " predDev=" + fmt(predictionMinDeviation)
                        + " streak=" + streak);
            }
        } else {
            coolDownScore(data);
            data.resetNoSlowViolationStreak();
        }
    }

    private boolean isGraceWindow(PlayerData data) {
        if (data.getPredictionContext().isRecentVelocity() || data.getPredictionContext().isRecentRodPull()) {
            return true;
        }
        long timeSinceVelocity = System.currentTimeMillis() - data.getLastVelocityAt();
        if (timeSinceVelocity < 500L) {
            return true;
        }
        if (data.getLastDeltaY() > 0.1D && data.getGroundTicks() > 0) {
            return true;
        }
        return data.isInSlotSwitchGrace();
    }

    private boolean isUsingItem(Player player, PlayerData data) {
        ItemStack hand = player.getItemInHand();
        if (hand == null || hand.getType() == Material.AIR) {
            return false;
        }
        Material type = hand.getType();
        long maxAge = plugin.getConfig().getLong("checks.NoSlow.use-packet-max-age-ms", 250L);
        boolean recentUsePacket = data.hasRecentUseItemPacket(maxAge);
        if (player.isBlocking()) {
            return true;
        }
        if (!recentUsePacket) {
            return false;
        }
        if (type == Material.BOW || type == Material.POTION || type == Material.MILK_BUCKET || type == Material.GOLDEN_APPLE) {
            return true;
        }
        return type.isEdible() || type == Material.MUSHROOM_SOUP || type == Material.RAW_FISH || type == Material.COOKED_FISH;
    }

    private static String fmt(double value) {
        return String.format(Locale.ROOT, "%.4f", value);
    }
}
