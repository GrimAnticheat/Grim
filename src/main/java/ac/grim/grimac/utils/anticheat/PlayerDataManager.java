package ac.grim.grimac.utils.anticheat;

import ac.grim.grimac.player.GrimPlayer;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {
    private final ConcurrentHashMap<Player, GrimPlayer> playerDataMap = new ConcurrentHashMap<>();

    @Nullable
    public GrimPlayer getPlayer(final Player player) {
        if (player == null) {
            LogUtil.warn("PacketEvents returned null for an event's player");
            return null;
        }
        return playerDataMap.get(player);
    }

    public void addPlayer(final GrimPlayer player) {
        playerDataMap.put(player.bukkitPlayer, player);
    }

    public void remove(final Player player) {
        playerDataMap.remove(player);
    }

    public Collection<GrimPlayer> getEntries() {
        return playerDataMap.values();
    }

    public int size() {
        return playerDataMap.size();
    }
}
