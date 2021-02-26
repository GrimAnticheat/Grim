package org.abyssmc.reaperac.events.anticheat;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import org.abyssmc.reaperac.GrimPlayer;
import org.abyssmc.reaperac.ReaperAC;
import org.abyssmc.reaperac.checks.movement.BaseMovementCheck;
import org.abyssmc.reaperac.checks.movement.MovementVelocityCheck;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GenericMovementCheck {
    List<PacketType> flyingPackets = Arrays.asList(PacketType.Play.Client.POSITION, PacketType.Play.Client.POSITION_LOOK,
            PacketType.Play.Client.LOOK, PacketType.Play.Client.FLYING);
    ProtocolManager manager;
    Plugin plugin;

    // Yeah... I know I lose a bit of performance from a list over a set, but it's worth it for consistency
    List<BaseMovementCheck> movementCheckListeners = new ArrayList<>();

    // YES I KNOW THIS CLASS IS TERRIBLE.
    // EARLIER TODAY I WANTED IT TO BE A MANAGER CLASS
    // LATER TODAY A CLASS THAT THINGS EXTEND
    // AND NOW IT'S BOTH SO THE CODE IS TERRIBLE!
    public GenericMovementCheck(Plugin plugin, ProtocolManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        registerPackets();
    }

    public void registerPackets() {
        manager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Client.POSITION) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                PacketContainer packet = event.getPacket();
                GrimPlayer player = ReaperAC.playerGrimHashMap.get(event.getPlayer());
                double x = packet.getDoubles().read(0);
                double y = packet.getDoubles().read(1);
                double z = packet.getDoubles().read(2);
                boolean onGround = packet.getBooleans().read(0);

                check(player, x, y, z, player.lastXRot, player.lastYRot, onGround);
            }
        });

        manager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Client.POSITION_LOOK) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                PacketContainer packet = event.getPacket();
                GrimPlayer player = ReaperAC.playerGrimHashMap.get(event.getPlayer());
                double x = packet.getDoubles().read(0);
                double y = packet.getDoubles().read(1);
                double z = packet.getDoubles().read(2);
                float xRot = packet.getFloat().read(0);
                float yRot = packet.getFloat().read(1);
                boolean onGround = packet.getBooleans().read(0);

                check(player, x, y, z, xRot, yRot, onGround);
            }
        });

        manager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Client.LOOK) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                PacketContainer packet = event.getPacket();
                GrimPlayer player = ReaperAC.playerGrimHashMap.get(event.getPlayer());
                float xRot = packet.getFloat().read(0);
                float yRot = packet.getFloat().read(1);
                boolean onGround = packet.getBooleans().read(0);

                check(player, player.lastX, player.lastY, player.lastZ, xRot, yRot, onGround);
            }
        });

        manager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Client.FLYING) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                PacketContainer packet = event.getPacket();
                GrimPlayer player = ReaperAC.playerGrimHashMap.get(event.getPlayer());
                boolean onGround = packet.getBooleans().read(0);

                check(player, player.lastX, player.lastY, player.lastZ, player.lastXRot, player.lastYRot, onGround);
            }
        });
    }

    // TODO: TERRIBLE CODE FIX THIS
    public void check(GrimPlayer player, double x, double y, double z, float xRot, float yRot, boolean onGround) {
        new MovementVelocityCheck(player, x, y, z, xRot, yRot, onGround);
    }

    public void registerCheck(BaseMovementCheck check) {
        movementCheckListeners.add(check);
    }
}
