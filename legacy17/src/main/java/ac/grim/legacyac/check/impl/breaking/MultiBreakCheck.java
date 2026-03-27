package ac.grim.legacyac.check.impl.breaking;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockBreakEvent;

public final class MultiBreakCheck extends Check {
    private final Map<UUID, BreakState> states = new ConcurrentHashMap<UUID, BreakState>();

    public MultiBreakCheck(LegacyAntiCheatPlugin plugin) {
        super(plugin, "MultiBreak");
    }

    public void onBreak(BlockBreakEvent event, PlayerData data) {
        if (!isEnabled() || isExempt(event.getPlayer(), data)) {
            return;
        }
        UUID uuid = event.getPlayer().getUniqueId();
        BreakState state = states.get(uuid);
        if (state == null) {
            state = new BreakState();
            BreakState existing = states.putIfAbsent(uuid, state);
            if (existing != null) {
                state = existing;
            }
        }

        long now = System.currentTimeMillis();
        Block block = event.getBlock();
        String current = block.getX() + ":" + block.getY() + ":" + block.getZ();
        if (state.lastBlockKey != null && !state.lastBlockKey.equals(current)
                && now - state.lastAt < plugin.getConfig().getLong("checks.MultiBreak.max-interval-ms", 25L)) {
            double buffer = increaseBuffer(data, 0.6D);
            if (buffer > plugin.getConfig().getDouble("checks.MultiBreak.buffer", 1.25D)) {
                flag(event.getPlayer(), data, 0.6D,
                        "last=" + state.lastBlockKey + " current=" + current + " dt=" + (now - state.lastAt));
            }
        } else {
            coolDownScore(data);
        }
        state.lastBlockKey = current;
        state.lastAt = now;
    }

    public void onPacketBreak(org.bukkit.entity.Player player, PlayerData data, PlayerData.QueuedBlockDigSnapshot snapshot) {
        if (!isEnabled() || isExempt(player, data)) {
            return;
        }
        UUID uuid = player.getUniqueId();
        BreakState state = states.get(uuid);
        if (state == null) {
            state = new BreakState();
            BreakState existing = states.putIfAbsent(uuid, state);
            if (existing != null) {
                state = existing;
            }
        }

        long now = System.currentTimeMillis();
        String current = snapshot.getX() + ":" + snapshot.getY() + ":" + snapshot.getZ();
        if (state.lastBlockKey != null && !state.lastBlockKey.equals(current)
                && now - state.lastAt < plugin.getConfig().getLong("checks.MultiBreak.max-interval-ms", 25L)) {
            double buffer = increaseBuffer(data, 0.6D);
            if (buffer > plugin.getConfig().getDouble("checks.MultiBreak.buffer", 1.25D)) {
                flag(player, data, 0.6D,
                        "last=" + state.lastBlockKey + " current=" + current + " dt=" + (now - state.lastAt));
            }
        } else {
            coolDownScore(data);
        }
        state.lastBlockKey = current;
        state.lastAt = now;
    }

    private static final class BreakState {
        private String lastBlockKey;
        private long lastAt;
    }
}
