package ac.grim.grimac.utils.anticheat;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.floodgate.FloodgateUtil;
import com.github.puregero.multilib.MultiLib;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.User;
import io.github.retrooper.packetevents.util.GeyserUtil;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {
    private final ConcurrentHashMap<User, GrimPlayer> playerDataMap = new ConcurrentHashMap<>();
    public final Collection<User> exemptUsers = Collections.synchronizedCollection(new HashSet<>());

    public GrimPlayer getPlayer(final Player player) {
        if (MultiLib.isExternalPlayer(player)) return null;

        // Is it safe to interact with this, or is this internal PacketEvents code?
        User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
        return playerDataMap.get(user);
    }

    public boolean shouldCheck(User user) {
        if (exemptUsers.contains(user)) return false;

        if (user.getUUID() != null) {
            // Geyser players don't have Java movement
            // Floodgate is the authentication system for Geyser on servers that use Geyser as a proxy instead of installing it as a plugin directly on the server
            if (GeyserUtil.isGeyserPlayer(user.getUUID()) || FloodgateUtil.isFloodgatePlayer(user.getUUID())) {
                exemptUsers.add(user);
                return false;
            }
            // Geyser formatted player string
            // This will never happen for Java players, as the first character in the 3rd group is always 4 (xxxxxxxx-xxxx-4xxx-xxxx-xxxxxxxxxxxx)
            if (user.getUUID().toString().startsWith("00000000-0000-0000-0009")) {
                exemptUsers.add(user);
                return false;
            }
        }

        return true;
    }

    @Nullable
    public GrimPlayer getPlayer(final User player) {
        if (!playerDataMap.containsKey(player) && shouldCheck(player)) {
            new IllegalStateException("Unable to find player " + player.getName() + " in player map, adding!").printStackTrace();
            GrimAPI.INSTANCE.getPlayerDataManager().addPlayer(player, new GrimPlayer(player));
        }
        return playerDataMap.get(player);
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
