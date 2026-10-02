package ac.grim.grimac.platform.bukkit;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.platform.api.Platform;
import ac.grim.grimac.platform.api.PlatformServer;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.platform.bukkit.initables.FoliaRegionTPSTracker;
import ac.grim.grimac.platform.bukkit.utils.reflection.FoliaRegionTPS;
import ac.grim.grimac.player.GrimPlayer;
import io.github.retrooper.packetevents.util.SpigotReflectionUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import java.util.UUID;

public class BukkitPlatformServer implements PlatformServer {

    @Override
    public String getPlatformImplementationString() {
        return Bukkit.getVersion();
    }

    @Override
    public void dispatchCommand(Sender sender, String command) {
        CommandSender commandSender = GrimACBukkitLoaderPlugin.LOADER.getBukkitSenderFactory().reverse(sender);
        Bukkit.dispatchCommand(commandSender, command);
    }

    @Override
    public Sender getConsoleSender() {
        return GrimACBukkitLoaderPlugin.LOADER.getBukkitSenderFactory().map(Bukkit.getConsoleSender());
    }

    @Override
    public void registerOutgoingPluginChannel(String name) {
        GrimACBukkitLoaderPlugin.LOADER.getServer().getMessenger().registerOutgoingPluginChannel(GrimACBukkitLoaderPlugin.LOADER, name);
    }

    @Override
    public double getTPS() {
        if (GrimAPI.INSTANCE.getPlatform() == Platform.FOLIA) {
            return Double.NaN;
        }
        return SpigotReflectionUtil.getTPS();
    }

    @Override
    public double getTPS(UUID playerId) {
        if (GrimAPI.INSTANCE.getPlatform() != Platform.FOLIA) {
            return getTPS();
        }
        if (playerId == null) {
            return Double.NaN;
        }
        if (!FoliaRegionTPS.hasRegionTpsApi()) {
            return FoliaRegionTPSTracker.getTPS(playerId);
        }
        try {
            return FoliaRegionTPS.regionTPS(locationOf(playerId));
        } catch (Exception e) {
            FoliaRegionTPS.warn(e);
            return Double.NaN;
        }
    }

    private static Location locationOf(UUID playerId) {
        GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(playerId);
        if (player == null) return null;

        UUID worldId = player.getWorldUID();
        World world = worldId == null ? null : Bukkit.getWorld(worldId);
        return world == null ? null : new Location(world, player.x, player.y, player.z);
    }
}
