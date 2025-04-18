package ac.grim.grimac.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@CommandAlias("grim|grimac")
public class GrimChannelDebugger extends BaseCommand implements Listener {

    // Map of player UUIDs to the set of channels received via plugin message packets
    private final ConcurrentHashMap<UUID, Set<String>> playerChannels = new ConcurrentHashMap<>();

    public GrimChannelDebugger() {
        // Register a packet listener to capture plugin message (custom payload) channels
        PacketEvents.getAPI().getEventManager().registerListener(new PacketListenerAbstract() {
            @Override
            public void onPacketReceive(PacketReceiveEvent event) {
                if (isPluginMessagePacket(event.getPacketType())) {
                    WrapperPlayClientPluginMessage packet = new WrapperPlayClientPluginMessage(event);
                    if (!(event.getPlayer() instanceof Player)) {
                        return;
                    }
                    UUID playerId = ((Player) event.getPlayer()).getUniqueId();
                    Set<String> channels = playerChannels.computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet());

                    // Handle Fabric channel registration/unregistration
                    if (packet.getChannelName().equals("minecraft:register") ||
                        packet.getChannelName().equals("minecraft:unregister")) {

                        String payload = new String(packet.getData(), StandardCharsets.UTF_8);
                        String[] fabricChannels = payload.split("\0");

                        if (packet.getChannelName().equals("minecraft:register")) {
                            Collections.addAll(channels, fabricChannels);
                        } else {
                            for (String channel : fabricChannels) {
                                channels.remove(channel);
                            }
                        }
                    } else {
                        // Add direct channel usage
                        channels.add(packet.getChannelName());
                    }
                }
            }
        });
    }

    private boolean isPluginMessagePacket(Object packetType) {
        return packetType == PacketType.Play.Client.PLUGIN_MESSAGE ||
               packetType == PacketType.Configuration.Client.PLUGIN_MESSAGE;
    }

    @Subcommand("debugchannel")
    @CommandPermission("grim.debugchannel")
    @CommandCompletion("@players")
    public void onDebugChannel(CommandSender sender, @Optional OnlinePlayer target) {
        Player targetPlayer = (target != null) ? target.getPlayer() :
                             ((sender instanceof Player) ? (Player) sender : null);

        if (targetPlayer == null) {
            sender.sendMessage("§cUsage: /grim debugchannel <player>");
            return;
        }

        Set<String> channels = playerChannels.getOrDefault(targetPlayer.getUniqueId(), Collections.emptySet());
        sender.sendMessage("§aRegistered Channels for " + targetPlayer.getName() + ":");
        if (channels.isEmpty()) {
            sender.sendMessage("§7No channels received.");
        } else {
            channels.forEach(channel -> sender.sendMessage("§7- §f" + channel));
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerChannels.remove(event.getPlayer().getUniqueId());
    }
}
