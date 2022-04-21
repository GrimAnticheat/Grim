package ac.grim.grimac.utils.anticheat;

import ac.grim.grimac.player.GrimPlayer;
import com.github.puregero.multilib.MultiLib;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.User;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {
    private final ConcurrentHashMap<User, GrimPlayer> playerDataMap = new ConcurrentHashMap<>();

    public GrimPlayer getPlayer(final Player player) {
        if (MultiLib.isExternalPlayer(player)) return null;

        // Is it safe to interact with this, or is this internal PacketEvents code?
        User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
        if (user == null) {
            LogUtil.warn("PacketEvents not injected for player " + player.getName() + " " + player.getUniqueId());
            return null;
        }
        return playerDataMap.get(user);
    }

    @Nullable
    public GrimPlayer getPlayer(final User player) {
        if (player == null) {
            new IllegalStateException("PacketEvents returned null for an event's user.  This is NEVER possible!").printStackTrace();
            return null;
        }

        GrimPlayer grimPlayer = playerDataMap.get(player);

        if (grimPlayer == null) {
            // Player teleport event gets called AFTER player join event
            new GrimPlayer(player);
            return playerDataMap.get(player);
        }

        return grimPlayer;
    }

    public void addPlayer(final User user, final GrimPlayer player) {
        playerDataMap.put(user, player);
    }

    public void remove(final User player) {
        playerDataMap.remove(player);
    }

    public Collection<GrimPlayer> getEntries() {
        return playerDataMap.values();
    }

    public int size() {
        return playerDataMap.size();
    }
}
