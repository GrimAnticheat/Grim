package ac.grim.grimac.platform.fabric;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.platform.api.PlatformServer;
import ac.grim.grimac.platform.api.player.PlatformPlayer;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.platform.fabric.player.FabricPlatformPlayer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class FabricPlatformServer implements PlatformServer {

    @Override
    public Collection<PlatformPlayer> getOnlinePlayers() {
        // Get the list of online players from the server
        List<ServerPlayerEntity> players = GrimACFabricLoaderPlugin.FABRIC_SERVER.getPlayerManager().getPlayerList();
        List<PlatformPlayer> platformPlayers = new ArrayList<>(players.size());

        // Convert each ServerPlayerEntity to PlatformPlayer
        for (ServerPlayerEntity player : players) {
            platformPlayers.add(new FabricPlatformPlayer(player));
        }

        return platformPlayers;
    }

    @Override
    public PlatformPlayer getPlayer(UUID uuid) {
        return GrimAPI.INSTANCE.getPlatformPlayerFactory().getFromUUID(uuid);
    }

    @Override
    public String getPlatformImplementationString() {
        // Return the Fabric server version
        return "Fabric " + FabricLoader.getInstance().getModContainer("fabricloader").get().getMetadata().getVersion().getFriendlyString() + " (MC: " + GrimACFabricLoaderPlugin.FABRIC_SERVER.getVersion() + ")";
    }

    @Override
    public void dispatchCommand(Sender sender, String command) {
        ServerCommandSource commandSource = GrimACFabricLoaderPlugin.PLUGIN.getFabricSenderFactory().reverse(sender);
        GrimACFabricLoaderPlugin.FABRIC_SERVER.getCommandManager().executeWithPrefix(commandSource, command);
    }

    @Override
    public Sender getConsoleSender() {
        ServerCommandSource consoleSource = GrimACFabricLoaderPlugin.FABRIC_SERVER.getCommandSource();
        return GrimACFabricLoaderPlugin.PLUGIN.getFabricSenderFactory().map(consoleSource);
    }

    @Override
    public void registerOutgoingPluginChannel(String bungeeCord) {
        throw new UnsupportedOperationException();
    }

    @Override
    public double getTPS() {
        return GrimACFabricLoaderPlugin.FABRIC_SERVER.getAverageTickTime();
    }
}
