package ac.grim.legacyac.check.impl;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import java.util.Locale;

public final class SpeedCheck extends Check {
    public SpeedCheck(LegacyAntiCheatPlugin plugin) {
        super(plugin, "Speed");
    }

    public void onMove(PlayerMoveEvent event, PlayerData data) {
        if (!isEnabled()) {
            return;
        }

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) {
            return;
        }

        Player player = event.getPlayer();
        if (isExempt(player, data)) {
            return;
        }
        if (player.isFlying() || player.getVehicle() != null) {
            return;
        }

        double horizontal = data.getLastDeltaXZ();
        double max = player.isSprinting() ? 0.37D : 0.30D;
        if (!player.isOnGround()) {
            max += 0.06D;
        }
        if (to.getBlock().getType() == Material.ICE || from.getBlock().getType() == Material.ICE) {
            max += 0.18D;
        }
        if (to.getBlock().getType() == Material.WEB || from.getBlock().getType() == Material.WEB) {
            max -= 0.10D;
        }

        PotionEffect speed = getPotion(player, PotionEffectType.SPEED);
        if (speed != null) {
            max += (speed.getAmplifier() + 1) * 0.055D;
        }

        if (horizontal > max) {
            double buffer = increaseBuffer(data, horizontal - max);
            if (buffer > plugin.getConfig().getDouble("checks.Speed.buffer", 0.24D)) {
                flag(player, data, horizontal - max, "h=" + format(horizontal) + " max=" + format(max));
            }
        } else if (player.isOnGround() && from.getY() == to.getY()) {
            data.setLastSafeLocation(to.clone());
        }
    }

    private PotionEffect getPotion(Player player, PotionEffectType type) {
        for (PotionEffect effect : player.getActivePotionEffects()) {
            if (effect.getType().equals(type)) {
                return effect;
            }
        }
        return null;
    }

    private String format(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }
}
