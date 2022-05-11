package ac.grim.grimac.manager;

import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfo;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SpectateManager {

    private final Map<UUID, PreviousState> spectatingPlayers = new ConcurrentHashMap<>();

    public boolean isSpectating(UUID uuid) {
        return spectatingPlayers.containsKey(uuid);
    }

    public boolean shouldHidePlayer(User receiver, WrapperPlayServerPlayerInfo.PlayerData playerData) {
        return playerData.getUser() != null
                && !playerData.getUser().getUUID().equals(receiver.getUUID())
                && spectatingPlayers.containsKey(playerData.getUser().getUUID());
    }

    public boolean enable(Player player) {
        if (spectatingPlayers.containsKey(player.getUniqueId())) return false;
        spectatingPlayers.put(player.getUniqueId(), new PreviousState(player.getGameMode(), player.getLocation()));
        return true;
    }

    public void disable(Player player) {
        PreviousState previousState = spectatingPlayers.get(player.getUniqueId());
        if (previousState != null) {
            player.teleport(previousState.location);
            player.setGameMode(previousState.gameMode);
        }
        handlePlayerStopSpectating(player.getUniqueId());
    }

    public void handlePlayerStopSpectating(UUID uuid) {
        spectatingPlayers.remove(uuid);
    }

    private static class PreviousState {
        public PreviousState(org.bukkit.GameMode gameMode, Location location) {
            this.gameMode = gameMode;
            this.location = location;
        }

        private final org.bukkit.GameMode gameMode;
        private final Location location;
    }

}
