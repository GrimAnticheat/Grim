package ac.grim.grimac.platform.bukkit;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.platform.api.Platform;
import ac.grim.grimac.platform.api.PlatformServer;
import ac.grim.grimac.platform.api.sender.Sender;
import io.github.retrooper.packetevents.util.SpigotReflectionUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;



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
        // Folia throws UnsupportedOperationException on calling getTPS(), there is no API for getting TPS on Folia
        if (GrimAPI.INSTANCE.getPlatform() == Platform.FOLIA) {
            return Double.NaN;
        }
        return SpigotReflectionUtil.getTPS();
    }
}
