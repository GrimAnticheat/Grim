package ac.grim.legacyac.check.impl.scaffold;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;

public final class FabricatedPlaceCheck extends Check {
    private static final double MAX_DOUBLE_ERROR = Math.ulp(30_000_000.0D) * 2.0D;

    public FabricatedPlaceCheck(LegacyAntiCheatPlugin plugin) {
        super(plugin, "FabricatedPlace");
    }

    public void onPlace(BlockPlaceEvent event, PlayerData data) {
        if (!isEnabled() || isExempt(event.getPlayer(), data)) {
            return;
        }
        if (!data.hasLastClientCursor()) {
            return;
        }
        if (System.currentTimeMillis() - data.getLastClientBlockPlacePacketAt() > 500L) {
            return;
        }
        Block against = event.getBlockAgainst();
        if (against == null) {
            return;
        }
        if (data.getLastClientPlaceX() != against.getX()
                || data.getLastClientPlaceY() != against.getY()
                || data.getLastClientPlaceZ() != against.getZ()) {
            return;
        }
        double minBound = 0.0D;
        double maxBound = 1.0D;
        double cursorX = data.getLastClientCursorX();
        double cursorY = data.getLastClientCursorY();
        double cursorZ = data.getLastClientCursorZ();
        boolean invalid = cursorX < minBound - MAX_DOUBLE_ERROR || cursorY < minBound - MAX_DOUBLE_ERROR || cursorZ < minBound - MAX_DOUBLE_ERROR
                || cursorX > maxBound + Math.ulp(1.0F) || cursorY > maxBound + Math.ulp(1.0F) || cursorZ > maxBound + Math.ulp(1.0F);
        if (invalid) {
            Player player = event.getPlayer();
            double buffer = increaseBuffer(data, 1.0D);
            if (buffer > plugin.getConfig().getDouble("checks.FabricatedPlace.buffer", 1.0D)) {
                flag(player, data, 1.0D,
                        "cursor=" + String.format(java.util.Locale.ROOT, "%.4f,%.4f,%.4f", cursorX, cursorY, cursorZ));
                event.setCancelled(true);
            }
        } else {
            coolDownScore(data);
        }
    }

    public void onPacketPlace(Player player, PlayerData data, PlayerData.QueuedBlockPlaceSnapshot snapshot) {
        if (!isEnabled() || isExempt(player, data)) {
            return;
        }
        double minBound = 0.0D;
        double maxBound = 1.0D;
        double cursorX = snapshot.getCursorX();
        double cursorY = snapshot.getCursorY();
        double cursorZ = snapshot.getCursorZ();
        boolean invalid = cursorX < minBound - MAX_DOUBLE_ERROR || cursorY < minBound - MAX_DOUBLE_ERROR
                || cursorZ < minBound - MAX_DOUBLE_ERROR
                || cursorX > maxBound + Math.ulp(1.0F) || cursorY > maxBound + Math.ulp(1.0F)
                || cursorZ > maxBound + Math.ulp(1.0F);
        if (invalid) {
            double buffer = increaseBuffer(data, 1.0D);
            if (buffer > plugin.getConfig().getDouble("checks.FabricatedPlace.buffer", 1.0D)) {
                flag(player, data, 1.0D,
                        "cursor=" + String.format(java.util.Locale.ROOT, "%.4f,%.4f,%.4f", cursorX, cursorY, cursorZ));
            }
        } else {
            coolDownScore(data);
        }
    }
}
