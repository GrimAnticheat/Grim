package ac.grim.grimac.platform.bukkit.player;

import ac.grim.grimac.platform.api.player.OfflinePlatformPlayer;
import lombok.RequiredArgsConstructor;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

@RequiredArgsConstructor
public class BukkitOfflinePlatformPlayer implements OfflinePlatformPlayer {
    private final OfflinePlayer offlinePlayer;

    @Override
    public boolean isOnline() {
        return offlinePlayer.isOnline();
    }

    @Override
    public @NotNull String getName() {
        return Objects.requireNonNull(offlinePlayer.getName());
    }

    @Override
    public @NotNull UUID getUniqueId() {
        return offlinePlayer.getUniqueId();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof OfflinePlatformPlayer player && this.getUniqueId().equals(player.getUniqueId());
    }
}
