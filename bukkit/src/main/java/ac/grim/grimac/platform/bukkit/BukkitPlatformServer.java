package ac.grim.grimac.platform.bukkit;

import ac.grim.grimac.platform.bukkit.player.BukkitPlatformPlayer;
import ac.grim.grimac.platform.api.PlatformServer;
import ac.grim.grimac.platform.api.player.PlatformPlayer;
import ac.grim.grimac.platform.api.sender.Sender;
import io.github.retrooper.packetevents.util.SpigotReflectionUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

public class BukkitPlatformServer implements PlatformServer {

    // TODO, super inefficient, fix later
    @Override
    public Collection<PlatformPlayer> getOnlinePlayers() {
        return Bukkit.getOnlinePlayers().stream()
                .map(BukkitPlatformPlayer::new)
                .collect(Collectors.toList());
    }

    @Override
    public PlatformPlayer getPlayer(UUID uuid) {
        return new BukkitPlatformPlayer(Bukkit.getPlayer(uuid));
    }

    @Override
    public String getPlatformImplementationString() {
        return Bukkit.getVersion();
    }

    @Override
    public void dispatchCommand(Sender sender, String command) {
        CommandSender commandSender = GrimACBukkitLoaderPlugin.PLUGIN.getBukkitSenderFactory().reverse(sender);
        Bukkit.dispatchCommand(commandSender, command);
    }

    @Override
    public Sender getConsoleSender() {
        return GrimACBukkitLoaderPlugin.PLUGIN.getBukkitSenderFactory().map(Bukkit.getConsoleSender());
    }

    @Override
    public void registerOutgoingPluginChannel(String bungeeCord) {
        GrimACBukkitLoaderPlugin.PLUGIN.getServer().getMessenger().registerOutgoingPluginChannel(GrimACBukkitLoaderPlugin.PLUGIN, "BungeeCord");
    }

    @Override
    public double getTPS() {
        return SpigotReflectionUtil.getTPS();
    }
}
