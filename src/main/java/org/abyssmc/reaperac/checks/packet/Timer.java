package org.abyssmc.reaperac.checks.packet;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

public class Timer {
    ProtocolManager manager;
    Plugin plugin;
    // this is shit and works with one player - fix your player data class you idiot
    int packetsReceived = 0;
    long lastSecond = 0;

    List<PacketType> flyingPackets = Arrays.asList(PacketType.Play.Client.POSITION, PacketType.Play.Client.POSITION_LOOK,
            PacketType.Play.Client.LOOK, PacketType.Play.Client.FLYING);

    public Timer(Plugin plugin, ProtocolManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        registerPackets();
    }

    public void registerPackets() {
        manager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, flyingPackets) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                packetsReceived++;
                long currentTime = Instant.now().getEpochSecond();

                if (currentTime != lastSecond) {
                    lastSecond = currentTime;

                    Bukkit.broadcastMessage("We got " + packetsReceived + " packets");

                    packetsReceived = 0;
                }
            }
        });
    }
}
