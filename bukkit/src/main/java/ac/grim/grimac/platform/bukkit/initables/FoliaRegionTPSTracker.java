package ac.grim.grimac.platform.bukkit.initables;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.manager.init.start.StartableInitable;
import ac.grim.grimac.platform.api.Platform;
import ac.grim.grimac.platform.bukkit.player.BukkitPlatformPlayer;
import ac.grim.grimac.platform.bukkit.utils.reflection.FoliaRegionTPS;
import ac.grim.grimac.platform.bukkit.utils.reflection.PaperUtils;
import ac.grim.grimac.player.GrimPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

public class FoliaRegionTPSTracker implements StartableInitable, Listener {

    private static final long UPDATE_INTERVAL_NANOS = 1_000_000_000L;

    @Override
    public void start() {
        if (GrimAPI.INSTANCE.getPlatform() != Platform.FOLIA) return;
        if (!FoliaRegionTPS.isSupported()) {
            FoliaRegionTPS.warnUnsupported();
            return;
        }
        if (!PaperUtils.registerTickEndEvent(this, this::onRegionTickEnd)) {
            FoliaRegionTPS.warnUnsupported();
        }
    }

    private void onRegionTickEnd() {
        try {
            long now = System.nanoTime();
            double regionTps = Double.NaN;
            boolean computed = false;

            for (GrimPlayer player : GrimAPI.INSTANCE.getPlayerDataManager().getEntries()) {
                if (now - player.regionTpsUpdatedAt < UPDATE_INTERVAL_NANOS) continue;
                if (!(player.platformPlayer instanceof BukkitPlatformPlayer platformPlayer)) continue;
                Player bukkitPlayer = platformPlayer.getNative();
                if (bukkitPlayer == null || !Bukkit.isOwnedByCurrentRegion(bukkitPlayer)) continue;

                if (!computed) {
                    regionTps = FoliaRegionTPS.currentRegionTPS(bukkitPlayer);
                    computed = true;
                }

                player.regionTpsUpdatedAt = now;
                if (Double.isFinite(regionTps)) player.regionTps = regionTps;
            }
        } catch (Exception e) {
            FoliaRegionTPS.warn(e);
        }
    }
}
