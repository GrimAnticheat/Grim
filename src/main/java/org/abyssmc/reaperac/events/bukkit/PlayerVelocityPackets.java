package org.abyssmc.reaperac.events.bukkit;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import org.abyssmc.reaperac.GrimPlayer;
import org.abyssmc.reaperac.ReaperAC;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

public class PlayerVelocityPackets implements Listener {
    ProtocolManager manager;
    Plugin plugin;

    public PlayerVelocityPackets(Plugin plugin, ProtocolManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        registerPackets();
    }

    public void registerPackets() {
        manager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.MONITOR, PacketType.Play.Server.ENTITY_VELOCITY) {
            @Override
            public void onPacketSending(PacketEvent event) {
                PacketContainer packet = event.getPacket();
                GrimPlayer player = ReaperAC.playerGrimHashMap.get(event.getPlayer());

                // This means we are not setting the velocity of the player
                if (packet.getIntegers().read(0) != event.getPlayer().getEntityId()) {
                    return;
                }

                double x = packet.getIntegers().read(1) / 8000d;
                double y = packet.getIntegers().read(2) / 8000d;
                double z = packet.getIntegers().read(3) / 8000d;

                Vector playerVelocity = new Vector(x, y, z);
                Bukkit.broadcastMessage("Adding " + playerVelocity);
                player.possibleKnockback.add(playerVelocity);

                for (Vector vector : player.possibleKnockback) {
                    Bukkit.broadcastMessage(ChatColor.AQUA + "Current vectors " + vector);
                }
            }
        });
    }
}
