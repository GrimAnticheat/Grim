package ac.grim.legacyac.check.impl.aim;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import ac.grim.legacyac.network.frame.MovementFrame;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;

public final class AimModulo360Check extends Check {
    private final Map<UUID, Float> lastYawDeltas = new ConcurrentHashMap<UUID, Float>();

    public AimModulo360Check(LegacyAntiCheatPlugin plugin) {
        super(plugin, "AimModulo360");
    }

    public void onMovementFrame(Player player, MovementFrame frame, PlayerData data) {
        if (!frame.hasLook() || isExempt(player, data)) {
            return;
        }
        float lastDelta = 0.0F;
        Float previous = lastYawDeltas.get(player.getUniqueId());
        if (previous != null) {
            lastDelta = previous.floatValue();
        }
        float yawDelta = Math.abs(data.getLastYawDelta());
        if (Math.abs(data.getLastYaw()) < 360.0F && yawDelta > 320.0F && Math.abs(lastDelta) < 30.0F) {
            double buffer = increaseBuffer(data, 1.0D);
            if (buffer > plugin.getConfig().getDouble("checks.AimModulo360.buffer", 1.25D)) {
                flag(player, data, 1.0D, "yawDelta=" + String.format(java.util.Locale.ROOT, "%.2f", yawDelta));
            }
        } else {
            coolDownScore(data);
        }
        lastYawDeltas.put(player.getUniqueId(), Float.valueOf(yawDelta));
    }
}
