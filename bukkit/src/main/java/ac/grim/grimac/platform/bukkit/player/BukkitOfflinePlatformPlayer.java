package ac.grim.grimac.platform.bukkit.player;

import ac.grim.grimac.platform.api.player.OfflinePlatformPlayer;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class BukkitOfflinePlatformPlayer implements OfflinePlatformPlayer {
    private final OfflinePlayer offlinePlayer;

    public BukkitOfflinePlatformPlayer(OfflinePlayer offlinePlayer) {
        this.offlinePlayer = offlinePlayer;
    }

    @Override
    public boolean isOnline() {
        return offlinePlayer.isOnline();
    }

    @Override
    public @NotNull String getName() {
        return offlinePlayer.getName();
    }

    @Override
    public @NotNull UUID getUniqueId() {
        return offlinePlayer.getUniqueId();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof OfflinePlatformPlayer offlinePlatformPlayer) {
            return this.getUniqueId().equals(offlinePlatformPlayer.getUniqueId());
        }
        return false;
    }
}
