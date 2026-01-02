package ac.grim.grimac.platform.bukkit.player;

import ac.grim.grimac.platform.api.player.AbstractPlatformPlayerFactory;
import ac.grim.grimac.platform.api.player.OfflinePlatformPlayer;
import ac.grim.grimac.platform.api.player.PlatformPlayer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;


public class BukkitPlatformPlayerFactory extends AbstractPlatformPlayerFactory<Player> {

    @Override
    protected Player getNativePlayer(@NotNull UUID uuid) {
        return Bukkit.getPlayer(uuid);
    }

    @Override
    protected Player getNativePlayer(@NotNull String name) {
        return Bukkit.getPlayer(name);
    }

    @Override
    protected PlatformPlayer createPlatformPlayer(@NotNull Player nativePlayer) {
        return new BukkitPlatformPlayer(nativePlayer);
    }

    @Override
    protected UUID getPlayerUUID(@NotNull Player nativePlayer) {
        return nativePlayer.getUniqueId();
    }

    // The cast is safe because Bukkit.getOnlinePlayers() is guaranteed to contain Player or its subtypes,
    // and we're only reading from it.
    @Override
    @SuppressWarnings("unchecked")
    protected Collection<Player> getNativeOnlinePlayers() {
        // Cast Collection<? extends Player> to Collection<Player>
        return (Collection<Player>) Bukkit.getOnlinePlayers();
    }

    @Override
    public OfflinePlatformPlayer getOfflineFromUUID(@NotNull UUID uuid) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        return new BukkitOfflinePlatformPlayer(offlinePlayer);
    }

    @Override
    public OfflinePlatformPlayer getOfflineFromName(@NotNull String name) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(name);
        return new BukkitOfflinePlatformPlayer(offlinePlayer);
    }

    @Override
    public Collection<OfflinePlatformPlayer> getOfflinePlayers() {
        OfflinePlayer[] offlinePlayers = Bukkit.getOfflinePlayers();
        BukkitOfflinePlatformPlayer[] offlinePlatformPlayers = new BukkitOfflinePlatformPlayer[offlinePlayers.length];
        for (int i = 0; i < offlinePlayers.length; i++) {
            offlinePlatformPlayers[i] = new BukkitOfflinePlatformPlayer(offlinePlayers[i]);
        }
        return Arrays.asList(offlinePlatformPlayers);
    }
}
