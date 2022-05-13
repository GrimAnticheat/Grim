package ac.grim.grimac.utils.anticheat;

import ac.grim.grimac.player.GrimPlayer;
import com.github.puregero.multilib.MultiLib;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.User;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {
    private final ConcurrentHashMap<UUID, GrimPlayer> playerDataMap = new ConcurrentHashMap<>();

    public GrimPlayer getPlayer(final Player player) {
        if (MultiLib.isExternalPlayer(player)) return null;
        return playerDataMap.get(player.getUniqueId());
    }

    @Nullable
    public GrimPlayer getPlayer(final User player) {
        return playerDataMap.get(player.getUUID());
    }

    @Nullable
    public GrimPlayer getPlayer(final UUID uuid) {
        return playerDataMap.get(uuid);
    }

    public void addPlayer(final User user, final GrimPlayer player) {
        playerDataMap.put(user.getUUID(), player);
    }

    public void remove(final User player) {
        playerDataMap.remove(player.getUUID());
    }

    public Collection<GrimPlayer> getEntries() {
        return playerDataMap.values();
    }

    public int size() {
        return playerDataMap.size();
    }
}
