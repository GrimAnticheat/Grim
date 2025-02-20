package ac.grim.grimac.manager;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.manager.init.Initable;
import ac.grim.grimac.player.GameMode;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import ac.grim.grimac.world.Location;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfo;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SpectateManager implements Initable {

    private final Map<UUID, PreviousState> spectatingPlayers = new ConcurrentHashMap<>();
    private final Set<UUID> hiddenPlayers = ConcurrentHashMap.newKeySet();
    private final Set<String> allowedWorlds = ConcurrentHashMap.newKeySet();

    private boolean checkWorld = false;

    @Override
    public void start() {
        allowedWorlds.clear();
        allowedWorlds.addAll(GrimAPI.INSTANCE.getConfigManager().getConfig().getStringListElse("spectators.allowed-worlds", new ArrayList<>()));
        checkWorld = !(allowedWorlds.isEmpty() || new ArrayList<>(allowedWorlds).get(0).isEmpty());
    }

    public boolean isSpectating(UUID uuid) {
        return spectatingPlayers.containsKey(uuid);
    }

    public boolean shouldHidePlayer(GrimPlayer receiver, WrapperPlayServerPlayerInfo.PlayerData playerData) {
        return playerData.getUser() != null
                && playerData.getUser().getUUID() != null
                && shouldHidePlayer(receiver, playerData.getUser().getUUID());
    }

    public boolean shouldHidePlayer(GrimPlayer receiver, UUID uuid) {
        return !Objects.equals(uuid, receiver.uuid) // don't hide to yourself
                && (spectatingPlayers.containsKey(uuid) || hiddenPlayers.contains(uuid)) //hide if you are a spectator
                && !(receiver.uuid != null && (spectatingPlayers.containsKey(receiver.uuid) || hiddenPlayers.contains(receiver.uuid))) // don't hide to other spectators
                && (!checkWorld || (receiver.platformPlayer != null && allowedWorlds.contains(receiver.platformPlayer.getWorld().getName()))); // hide if you are in a specific world
    }

    public boolean enable(GrimPlayer player) {
        if (spectatingPlayers.containsKey(player.getUniqueId())) return false;
        spectatingPlayers.put(player.getUniqueId(), new PreviousState(player.getGameMode(), player.getLocation()));
        return true;
    }

    public void onLogin(GrimPlayer player) {
        hiddenPlayers.add(player.getUniqueId());
    }

    public void onQuit(GrimPlayer player) {
        hiddenPlayers.remove(player.getUniqueId());
        handlePlayerStopSpectating(player.getUniqueId());
    }

    // only call this synchronously
    public void disable(GrimPlayer player, boolean teleportBack) {
        PreviousState previousState = spectatingPlayers.get(player.getUniqueId());
        if (previousState != null) {
            if (teleportBack && previousState.location.isWorldLoaded()) {
                player.platformPlayer.teleportAsync(previousState.location).thenAccept(bool -> {
                    if (bool) {
                        onDisable(previousState, player);
                    } else {
                        MessageUtil.sendMessage(player, Component.text("Teleport failed, please try again.", NamedTextColor.RED));
                    }
                });
            } else {
                onDisable(previousState, player);
            }
        }
    }

    private void onDisable(PreviousState previousState, GrimPlayer player) {
        player.setGameMode(previousState.gameMode);
        handlePlayerStopSpectating(player.getUniqueId());
    }

    public void handlePlayerStopSpectating(UUID uuid) {
        spectatingPlayers.remove(uuid);
    }

    private record PreviousState(GameMode gameMode, Location location) {}
}
