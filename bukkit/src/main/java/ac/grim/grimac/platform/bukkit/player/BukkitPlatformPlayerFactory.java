package ac.grim.grimac.platform.bukkit.player;

import ac.grim.grimac.platform.api.player.AbstractPlatformPlayerFactory;
import ac.grim.grimac.platform.api.player.PlatformPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.UUID;


public class BukkitPlatformPlayerFactory extends AbstractPlatformPlayerFactory<Player> {
    @Override
    protected Player getNativePlayer(@NotNull UUID uuid) {
        return Bukkit.getPlayer(uuid);
    }

    @Override
    protected PlatformPlayer createPlatformPlayer(@NotNull Player nativePlayer) {
        return new BukkitPlatformPlayer(nativePlayer);
    }

    @Override
    protected boolean isNativePlayerType(@NotNull Object playerObject) {
        return playerObject instanceof Player;
    }

    @Override
    protected UUID getPlayerUUID(@NotNull Player nativePlayer) {
        return nativePlayer.getUniqueId();
    }

    @Override
    protected Class<Player> getNativePlayerClass() {
        return Player.class;
    }

    // The cast is safe because Bukkit.getOnlinePlayers() is guaranteed to contain Player or its subtypes,
    // and we're only reading from it.
    @Override
    @SuppressWarnings("unchecked")
    protected Collection<Player> getNativeOnlinePlayers() {
        // Cast Collection<? extends Player> to Collection<Player>
        return (Collection<Player>) Bukkit.getOnlinePlayers();
    }
}
